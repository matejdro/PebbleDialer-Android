package com.matejdro.pebbledialer.callactions;

import com.matejdro.pebbledialer.modules.CallModule;

public class DummyAction extends CallAction
{
    public static final int DUMMY_ACTION_ID = 999;


    public DummyAction(CallModule callModule)
    {
        super(callModule);

    }

    @Override
    public void executeAction()
    {
    }

    @Override
    public int getIcon()
    {
        return CallAction.ICON_BLANK;
    }

    public static DummyAction get(CallModule callModule)
    {
        return (DummyAction) callModule.getCallAction(DUMMY_ACTION_ID);
    }
}
