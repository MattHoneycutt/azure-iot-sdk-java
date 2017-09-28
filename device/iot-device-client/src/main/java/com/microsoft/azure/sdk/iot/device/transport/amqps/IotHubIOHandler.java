/*
*  Copyright (c) Microsoft. All rights reserved.
*  Licensed under the MIT license. See LICENSE file in the project root for full license information.
*/

package com.microsoft.azure.sdk.iot.device.transport.amqps;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Transport;
import org.apache.qpid.proton.reactor.Reactor;
import org.apache.qpid.proton.reactor.impl.Address;
import org.apache.qpid.proton.reactor.impl.IOHandler;
import org.apache.qpid.proton.reactor.impl.ReactorImpl;

public class IotHubIOHandler extends IOHandler
{
    // pni_handle_open(...) from connection.c
    private void handleOpen(Reactor reactor, Event event) {
        Connection connection = event.getConnection();
        if (connection.getRemoteState() != EndpointState.UNINITIALIZED) {
            return;
        }
        // Outgoing Reactor connections set the virtual host automatically using the
        // following rules:
        String vhost = connection.getHostname();
        if (vhost == null) {
            // setHostname never called, use the host from the connection's
            // socket address as the default virtual host:
            String conAddr = reactor.getConnectionAddress(connection);
            if (conAddr != null) {
                Address addr = new Address(conAddr);
                connection.setHostname(addr.getHost());
            }
        } else if (vhost.isEmpty()) {
            // setHostname called explictly with a null string. This allows
            // the application to completely avoid sending a virtual host
            // name
            connection.setHostname(null);
        } else {
            // setHostname set by application - use it.
        }

        Transport transport = Proton.transport();
        transport.bind(connection);
    }

    @Override
    public void onUnhandled(Event event)
    {
        ReactorImpl reactor = (ReactorImpl)event.getReactor();
        if (event.getType() == Event.Type.CONNECTION_LOCAL_OPEN)
        {
            handleOpen(reactor, event);
        }
        else
        {
            super.onUnhandled(event);
        }
    }
}
