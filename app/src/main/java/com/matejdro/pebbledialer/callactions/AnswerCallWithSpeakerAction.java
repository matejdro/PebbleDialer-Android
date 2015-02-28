package com.matejdro.pebbledialer.callactions;

import com.matejdro.pebbledialer.modules.CallModule;

public class AnswerCallWithSpeakerAction extends CallAction
{
    public static final int ANSWER_WITH_SPEAKER_ACTION_ID = 5;

    private boolean enableSpeakerImmediately = false;

    public AnswerCallWithSpeakerAction(CallModule callModule)
    {
        super(callModule);
    }


    @Override
    public void executeAction()
    {
        if (getCallModule().getCallState() != CallModule.CallState.RINGING)
            return;

        enableSpeakerImmediately = true;
        AnswerCallAction.get(getCallModule()).executeAction();
    }

    @Override
    public void onCallEnd()
    {
        enableSpeakerImmediately = false; //Reset intent (there will be new intent at next call)
    }

    @Override
    public void onPhoneOffhook()
    {
        if (enableSpeakerImmediately)
        {
            ToggleSpeakerAction speakerAction = ToggleSpeakerAction.get(getCallModule());

            if (!speakerAction.isSpeakerphoneEnabled())
                speakerAction.executeAction();
        }
    }

    @Override
    public int getIcon()
    {
        return CallAction.ICON_BUTTON_ANSWER;
    }

    public static AnswerCallWithSpeakerAction get(CallModule callModule)
    {
        return (AnswerCallWithSpeakerAction) callModule.getCallAction(ANSWER_WITH_SPEAKER_ACTION_ID);
    }
}
