package com.matejdro.pebbledialer.modules;

import android.util.SparseArray;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebbledialer.DataReceiver;
import com.matejdro.pebbledialer.PebbleTalkerService;
import com.matejdro.pebbledialer.pebble.PebbleCommunication;
import com.matejdro.pebbledialer.ui.ContactGroupsPickerDialog;
import com.matejdro.pebbledialer.util.ListSerialization;
import com.matejdro.pebbledialer.pebble.WatchappHandler;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import timber.log.Timber;

/**
 * Created by Matej on 29.11.2014.
 */
public class SystemModule extends CommModule
{
    public static int MODULE_SYSTEM = 0;

    public static final UUID UNKNOWN_UUID = new UUID(0, 0);
    public static final UUID MAIN_MENU_UUID = UUID.fromString("dec0424c-0625-4878-b1f2-147e57e83688");

    private Callable<Boolean> runOnNext;
    private UUID currentRunningApp;

    private List<ContactGroupsPickerDialog.ContactGroup> pickedContactGroups;
    private int nextGroupNameToSend = -1;

    private int closeTries = 0;

    public SystemModule(PebbleTalkerService service)
    {
        super(service);
        runOnNext = null;
    }

    @Override
    public boolean sendNextMessage()
    {
        if (runOnNext != null)
        {
            Callable<Boolean> oldRunOnNext = runOnNext;

            try
            {
                runOnNext.call();
                if (runOnNext == oldRunOnNext)
                    runOnNext = null;

                return true;
            } catch (Exception e)
            {
                e.printStackTrace();
                return false;
            }
        }

        if (nextGroupNameToSend != -1)
        {
            sendGroupNames();
            return true;
        }

        return false;

    }

    private void sendGroupNames()
    {
        if (nextGroupNameToSend >= pickedContactGroups.size())
        {
            nextGroupNameToSend = -1;
            return;
        }

        PebbleDictionary packet = new PebbleDictionary();
        packet.addUint8(0, (byte) 0);
        packet.addUint8(1, (byte) 2);
        packet.addUint8(2, (byte) nextGroupNameToSend);

        int num = Math.min(nextGroupNameToSend + 3, pickedContactGroups.size() - 1) - nextGroupNameToSend;
        for (int i = 0; i <= num; i++)
        {
            packet.addString(3 + i, pickedContactGroups.get(nextGroupNameToSend + i).getName());
        }

        nextGroupNameToSend += 3;

        getService().getPebbleCommunication().sendToPebble(packet);
    }

    private void sendConfig()
    {
        PebbleDictionary data = new PebbleDictionary();

        List<Integer> pickedGroupIds = ListSerialization.loadIntegerList(getService().getGlobalSettings(), "displayedGroupsListNew");
        pickedContactGroups = ContactGroupsPickerDialog.getSpecificContactGroups(getService(), pickedGroupIds);

        byte[] configBytes = new byte[13];
        configBytes[0] = (byte) (WatchappHandler.SUPPORTED_PROTOCOL >>> 0x08);
        configBytes[1] = (byte) WatchappHandler.SUPPORTED_PROTOCOL;

        boolean callWaiting = CallModule.get(getService()).getCallState() != CallModule.CallState.NO_CALL;

        byte flags = 0;
        flags |= (byte) (callWaiting ? 0x01 : 0);
        flags |= (byte) (getService().getGlobalSettings().getBoolean("closeToLastApp", false) ? 0x02 : 0);
        flags |= (byte) (getService().getGlobalSettings().getBoolean("skipGroupFiltering", false) ? 0x04 : 0);

        configBytes[2] = flags;
        configBytes[3] = (byte) pickedContactGroups.size();

        data.addUint8(0, (byte) 0);
        data.addUint8(1, (byte) 0);
        data.addBytes(2, configBytes);

        Timber.d("Sending config... CallWaiting=" + callWaiting);

        getService().getPebbleCommunication().sendToPebble(data);

        if (!callWaiting && pickedContactGroups.size() > 0)
            nextGroupNameToSend = 0;
    }

    private void sendConfigInvalidVersion(int version)
    {
        PebbleDictionary data = new PebbleDictionary();

        byte[] configBytes = new byte[13];
        configBytes[0] = (byte) (WatchappHandler.SUPPORTED_PROTOCOL >>> 0x08);
        configBytes[1] = (byte) WatchappHandler.SUPPORTED_PROTOCOL;

        data.addUint8(0, (byte) 0);
        data.addUint8(1, (byte) 0);
        data.addBytes(2, configBytes);

        Timber.d("Sending version mismatch config...");

        getService().getPebbleCommunication().sendToPebble(data);

        runOnNext = new Callable<Boolean>()
        {
            @Override
            public Boolean call() throws Exception
            {
                //Pretend that I sent new message to prevent other modules sending potentially unsupported messages
                return true;
            }
        };

        WatchappHandler.showUpdateNotification(getService());
    }

    /**
     * Pre 2.2 watchapp does not have update screen so we display fake call screen
     */
    private void sendFakeCall()
    {
        PebbleDictionary data = new PebbleDictionary();

        data.addUint8(0, (byte) 5);

        data.addString(1, "Old PebbleDialer");
        data.addString(2, "");

        data.addString(3, "Check phone");

        byte[] parameters = new byte[4];
        parameters[0] = 1;
        parameters[3] = 1;

        data.addBytes(4, parameters);

        data.addUint16(5, (short) 0);

        getService().getPebbleCommunication().sendToPebble(data);

        runOnNext = new Callable<Boolean>()
        {
            @Override
            public Boolean call() throws Exception
            {
                //Pretend that I sent new message to prevent other modules sending potentially unsupported messages
                return true;
            }
        };

        WatchappHandler.showUpdateNotification(getService());
    }


    private void gotMessagePebbleOpened(PebbleDictionary message)
    {
        closeTries = 0;

        int version = 0;
        if (message.contains(2))
            version = message.getUnsignedIntegerAsLong(2).intValue();

        final int finalVersion = version;

        Timber.d("Version " + version);

        if (version == WatchappHandler.SUPPORTED_PROTOCOL)
        {
            runOnNext = new Callable<Boolean>()
            {
                @Override
                public Boolean call() throws Exception
                {
                    sendConfig();
                    return true;
                }
            };

            int pebblePlatform = message.getUnsignedIntegerAsLong(3).intValue();
            getService().getPebbleCommunication().setConnectedPebblePlatform(pebblePlatform);
            Timber.d("Pebble Platform: " + pebblePlatform);

            SparseArray<CommModule> modules = getService().getAllModules();
            for (int i = 0 ; i < modules.size(); i++)
                modules.valueAt(i).pebbleAppOpened();
        }
        else if (version == 0)
        {
            runOnNext = new Callable<Boolean>()
            {
                @Override
                public Boolean call() throws Exception
                {
                    sendFakeCall();
                    return true;
                }
            };
        }
        else
        {
            runOnNext = new Callable<Boolean>()
            {
                @Override
                public Boolean call()
                {
                    sendConfigInvalidVersion(finalVersion);
                    return true;
                }
            };
        }


        PebbleCommunication communication = getService().getPebbleCommunication();
        communication.queueModulePriority(this);
        communication.resetBusy();
        communication.sendNext();
    }

    private void gotMessageMenuItem(PebbleDictionary message)
    {
        int entry = message.getUnsignedIntegerAsLong(2).intValue();

        if (entry == 0) //Call log
            CallLogModule.get(getService()).beginSending();
        else if (entry == 1) //All contacts
            ContactsModule.get(getService()).beginSending(-1);
        else //Groups
        {
            int groupId = entry - 2;
            if (pickedContactGroups == null || groupId < 0 || groupId >= pickedContactGroups.size())
            {
                Timber.w("Got invalid group ID from main menu!");
                return;
            }

            int group = pickedContactGroups.get(groupId).getId();
            ContactsModule.get(getService()).beginSending(group);
        }
    }

    @Override
    public void gotMessageFromPebble(PebbleDictionary message)
    {
        int id = 0;
        if (message.contains(1)) //Open message from older Pebble app does not have entry at 1.
            id = message.getUnsignedIntegerAsLong(1).intValue();

        Timber.d("system packet " + id);

        switch (id)
        {
            case 0: //Pebble opened
                gotMessagePebbleOpened(message);
                break;
            case 1: //Menu entry picked
                gotMessageMenuItem(message);
                break;
            case 2: //Close me
                closeApp();
                break;


        }
    }

    public void updateCurrentlyRunningApp()
    {
        UUID newApp = getService().getDeveloperConnection().getCurrentRunningApp();

        if (newApp != null && (!newApp.equals(DataReceiver.dialerUUID) || currentRunningApp == null) && !newApp.equals(UNKNOWN_UUID))
        {
            currentRunningApp = newApp;
        }
    }

    public UUID getCurrentRunningApp()
    {
        return currentRunningApp;
    }

    public void openApp()
    {
        updateCurrentlyRunningApp();
        PebbleKit.startAppOnPebble(getService(), DataReceiver.dialerUUID);
    }

    public void closeApp()
    {
        Timber.d("CloseApp " + currentRunningApp);

        if (getService().getGlobalSettings().getBoolean("closeToLastApp", false) && canCloseToApp(currentRunningApp) && closeTries < 2)
            PebbleKit.startAppOnPebble(getService(), currentRunningApp);
        else
            PebbleKit.closeAppOnPebble(getService(), DataReceiver.dialerUUID);

        closeTries++;
    }

    public void startClosing()
    {
        runOnNext = new Callable<Boolean>()
        {
            @Override
            public Boolean call() throws Exception
            {
                PebbleDictionary data = new PebbleDictionary();

                data.addUint8(0, (byte) 0);
                data.addUint8(1, (byte) 1);

                Timber.d("Sending close window packet...");

                getService().getPebbleCommunication().sendToPebble(data);
                closeApp();

                return true;
            }
        };

        PebbleCommunication communication = getService().getPebbleCommunication();
        communication.queueModulePriority(this);
        communication.sendNext();
    }

    private static boolean canCloseToApp(UUID uuid)
    {
        return uuid != null && !uuid.equals(DataReceiver.dialerUUID) && !uuid.equals(MAIN_MENU_UUID) && !uuid.equals(UNKNOWN_UUID);
    }

    public static SystemModule get(PebbleTalkerService service)
    {
        return (SystemModule) service.getModule(MODULE_SYSTEM);
    }
}
