package com.jensjansson.ce.bot;

import com.vaadin.collaborationengine.ActivationHandler;
import com.vaadin.collaborationengine.ConnectionContext;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

public class EagerConnectionContext implements ConnectionContext {
    @Override
    public Registration setActivationHandler(
            ActivationHandler activationHandler) {
        activationHandler.setActive(true);
        return null;
    }

    @Override
    public void dispatchAction(Command command) {
        command.execute();
    }
}
