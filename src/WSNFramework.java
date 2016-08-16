import de.tum.in.net.WSNDataFramework.WSNApp;
import de.tum.in.net.WSNDataFramework.CUSTOM.MartinTinyOSECCDriver;
import de.tum.in.net.WSNDataFramework.CUSTOM.PhilippSSHProtocol;
import de.tum.in.net.WSNDataFramework.CUSTOM.TUMI8Adapter;
import de.tum.in.net.WSNDataFramework.CUSTOM.ThomasTinyOSDTLSDriver;
import de.tum.in.net.WSNDataFramework.CUSTOM.TinyIPFIXProtocol;
import de.tum.in.net.WSNDataFramework.Modules.Contiki.WSNContikiModule;
import de.tum.in.net.WSNDataFramework.Modules.Cosm.WSNCosmModule;
import de.tum.in.net.WSNDataFramework.Modules.FileLogger.WSNFileLoggerModule;
import de.tum.in.net.WSNDataFramework.Modules.HTTPServer.WSNHTTPServerModule;
import de.tum.in.net.WSNDataFramework.Modules.IPFIX.WSNTinyIPFIXModule;
import de.tum.in.net.WSNDataFramework.Modules.SSHServer.WSNSSHServerModule;
import de.tum.in.net.WSNDataFramework.Modules.TinyOS.TinyOSHelperModule;
import de.tum.in.net.WSNDataFramework.Modules.Web.WSNWebModule;
import de.tum.in.net.WSNDataFramework.Protocols.IPFIXEnricherProtocol;
import de.tum.in.net.WSNDataFramework.Protocols.IPFIXProtocol;
import de.tum.in.net.WSNDataFramework.Protocols.TinyToProtocol;





public class WSNFramework {

	/**
	 * Example WSN-Data-Framework Application
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		try {
			/*** create a WSN instance ***/
			//WSNApp app = new WSNApp(); 										// WSNApp with default paths
			WSNApp app = new WSNApp("/home/livio/Desktop/database", "/tmp");	// WSNApp with specific storage paths
			//app.addModule(new TestWSNDriverModule());							// Test WSN-Driver
			/*app.setDriver(														// TinyOS WSN-Driver
					//new TinyOSDriver()                     //in if no OPAL in use
					//new ThomasTinyOSDTLSDriver()			// for Thomas' DTLS
					new MartinTinyOSECCDriver()			// for Martin's ECC
					);
			
			app.setProtocolStack(												// Protocol Stack, attaches the actual WSN to the App
					new PhilippSSHProtocol(),
					new TinyToProtocol(),
					new TinyIPFIXProtocol(),
					new IPFIXProtocol(),
					new IPFIXEnricherProtocol("conf/tinyIPFIX-conf.xml"),
					new TUMI8Adapter()
					);
			*/
			//app.addModule(new WSNTinyIPFIXModule("conf/tinyIPFIX-conf.xml",0));
			System.out.println("WSNApp up");

			/*** add modules ***/
			// attach the actual WSN
			//app.addModule(new TinyOSHelperModule("/opt/tinyos-2.1.2"));											// TinyOS GUI
			app.addModule(new WSNContikiModule());																// Contiki

			// User Interface
			app.addModule(new WSNHTTPServerModule(8000, "html"));												// HTTP/HTML GUI

			//WSNSSHServerModule sshServer = new WSNSSHServerModule(2222);										// SSH UI
			//sshServer.getAuthenticator().addPassword(null, null);												// allowing any username+password combination to access it
			//app.addModule(sshServer);
			
			app.addModule(new WSNTinyIPFIXModule("/home/livio/WSNFrameworkContiki/conf/newTelosB.xml", 0));
			
			//WebMaDa
			app.addModule(new WSNWebModule());

			// Logger
			app.addModule(new WSNFileLoggerModule());															// TXT logging
			app.addModule(new WSNCosmModule()); //("ratatatata","fetY7Y1WCQNWGMEhCIbdZd_HiRbP-ODVxJIcNvppzVA"));																	// Cosm Upload

			/*** wait for the WSN to shutdown ***/
			System.out.println("waiting for WSNApp to shut down");
			try {
				app.waitForShutdown();
			} catch(Exception e) {
			}
			System.out.println("WSN down");

		} catch (Exception e) {
			System.out.println("CRASH: " + e.getMessage());
			e.printStackTrace();
		}
	}

}
