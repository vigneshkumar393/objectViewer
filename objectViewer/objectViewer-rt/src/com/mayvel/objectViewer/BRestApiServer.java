package com.mayvel.objectViewer;

import com.mayvel.objectViewer.route.BmsRoute;
import com.sun.net.httpserver.HttpServer;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import java.io.IOException;
import java.net.InetSocketAddress;

@NiagaraType
public class BRestApiServer extends BComponent {
//region /*+ ------------ BEGIN BAJA AUTO GENERATED CODE ------------ +*/
//@formatter:off
/*@ $com.mayvel.kpiDashboard.BRestApiServer(2979906276)1.0$ @*/
/* Generated Mon Nov 24 14:45:53 IST 2025 by Slot-o-Matic (c) Tridium, Inc. 2012-2025 */

  //region Type

  @Override
  public Type getType() { return TYPE; }
  public static final Type TYPE = Sys.loadType(BRestApiServer.class);

  //endregion Type

//@formatter:on
//endregion /*+ ------------ END BAJA AUTO GENERATED CODE -------------- +*/

    private HttpServer server;

    /*+ ----------------- START BAJA AUTO GENERATED CODE ----------------- +*/

    /*+ ------------------ END BAJA AUTO GENERATED CODE ------------------ +*/

    @Override
    public void started() throws Exception {
        super.started();
        startServer();
    }

    @Override
    public void stopped() throws Exception {
        stopServer();
        super.stopped();
    }

    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(8000), 0);
        BComponent parent = (BComponent) this.getParent();
        // register all routes
        BmsRoute.registerRoutes(server, parent);

        server.setExecutor(null);
        server.start();
        System.out.println("✅ Niagara HTTP Server started at http://localhost:8000/");
    }

    public void stopServer() {
        if (server != null) {
            server.stop(0);
            System.out.println("🛑 Niagara HTTP Server stopped.");
        }
    }
}
