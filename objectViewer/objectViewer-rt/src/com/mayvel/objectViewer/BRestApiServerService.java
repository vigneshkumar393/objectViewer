package com.mayvel.objectViewer;

import com.mayvel.objectViewer.route.*;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.*;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyStore;

/**
 * Custom Niagara Service that can be added to the Services folder.
 */
@NiagaraProperty(
        name = "username",
        type = "String",
        defaultValue = "",
        flags = 0
)
@NiagaraProperty(
        name = "password",
        type = "String",
        defaultValue = "",
        flags = 0
)
@NiagaraProperty(
        name = "certificateFileName",
        type = "String",
        defaultValue = "",
        flags = Flags.READONLY | Flags.SUMMARY
)
@NiagaraProperty(
        name = "certificatePassword",
        type = "String",
        defaultValue = "",
        flags = Flags.READONLY | Flags.SUMMARY
)

@NiagaraProperty(
        name = "validationOut",
        type = "String",
        defaultValue = "",
        flags = Flags.READONLY | Flags.SUMMARY
)
@NiagaraProperty(
        name = "savedPoints",
        type = "String",
        defaultValue = "",
        flags = Flags.READONLY | Flags.SUMMARY,
        facets = {
                @Facet(name = "BFacets.MULTI_LINE", value = "BBoolean.TRUE"),
        }
)

@NiagaraType
public class BRestApiServerService extends BAbstractService
{
    private HttpServer server;
//region /*+ ------------ BEGIN BAJA AUTO GENERATED CODE ------------ +*/
//@formatter:off
/*@ $com.mayvel.objectViewer.BRestApiServerService(3231802199)1.0$ @*/
/* Generated Thu May 07 10:18:03 IST 2026 by Slot-o-Matic (c) Tridium, Inc. 2012-2026 */

  //region Property "username"

  /**
   * Slot for the {@code username} property.
   * Custom Niagara Service that can be added to the Services folder.
   * @see #getUsername
   * @see #setUsername
   */
  public static final Property username = newProperty(0, "", null);

  /**
   * Get the {@code username} property.
   * Custom Niagara Service that can be added to the Services folder.
   * @see #username
   */
  public String getUsername() { return getString(username); }

  /**
   * Set the {@code username} property.
   * Custom Niagara Service that can be added to the Services folder.
   * @see #username
   */
  public void setUsername(String v) { setString(username, v, null); }

  //endregion Property "username"

  //region Property "password"

  /**
   * Slot for the {@code password} property.
   * @see #getPassword
   * @see #setPassword
   */
  public static final Property password = newProperty(0, "", null);

  /**
   * Get the {@code password} property.
   * @see #password
   */
  public String getPassword() { return getString(password); }

  /**
   * Set the {@code password} property.
   * @see #password
   */
  public void setPassword(String v) { setString(password, v, null); }

  //endregion Property "password"

  //region Property "certificateFileName"

  /**
   * Slot for the {@code certificateFileName} property.
   * @see #getCertificateFileName
   * @see #setCertificateFileName
   */
  public static final Property certificateFileName = newProperty(Flags.READONLY | Flags.SUMMARY, "", null);

  /**
   * Get the {@code certificateFileName} property.
   * @see #certificateFileName
   */
  public String getCertificateFileName() { return getString(certificateFileName); }

  /**
   * Set the {@code certificateFileName} property.
   * @see #certificateFileName
   */
  public void setCertificateFileName(String v) { setString(certificateFileName, v, null); }

  //endregion Property "certificateFileName"

  //region Property "certificatePassword"

  /**
   * Slot for the {@code certificatePassword} property.
   * @see #getCertificatePassword
   * @see #setCertificatePassword
   */
  public static final Property certificatePassword = newProperty(Flags.READONLY | Flags.SUMMARY, "", null);

  /**
   * Get the {@code certificatePassword} property.
   * @see #certificatePassword
   */
  public String getCertificatePassword() { return getString(certificatePassword); }

  /**
   * Set the {@code certificatePassword} property.
   * @see #certificatePassword
   */
  public void setCertificatePassword(String v) { setString(certificatePassword, v, null); }

  //endregion Property "certificatePassword"

  //region Property "validationOut"

  /**
   * Slot for the {@code validationOut} property.
   * @see #getValidationOut
   * @see #setValidationOut
   */
  public static final Property validationOut = newProperty(Flags.READONLY | Flags.SUMMARY, "", null);

  /**
   * Get the {@code validationOut} property.
   * @see #validationOut
   */
  public String getValidationOut() { return getString(validationOut); }

  /**
   * Set the {@code validationOut} property.
   * @see #validationOut
   */
  public void setValidationOut(String v) { setString(validationOut, v, null); }

  //endregion Property "validationOut"

  //region Property "savedPoints"

  /**
   * Slot for the {@code savedPoints} property.
   * @see #getSavedPoints
   * @see #setSavedPoints
   */
  public static final Property savedPoints = newProperty(Flags.READONLY | Flags.SUMMARY, "", BFacets.make(BFacets.MULTI_LINE, BBoolean.TRUE));

  /**
   * Get the {@code savedPoints} property.
   * @see #savedPoints
   */
  public String getSavedPoints() { return getString(savedPoints); }

  /**
   * Set the {@code savedPoints} property.
   * @see #savedPoints
   */
  public void setSavedPoints(String v) { setString(savedPoints, v, null); }

  //endregion Property "savedPoints"

  //region Type

  @Override
  public Type getType() { return TYPE; }
  public static final Type TYPE = Sys.loadType(BRestApiServerService.class);

  //endregion Type

//@formatter:on
//endregion /*+ ------------ END BAJA AUTO GENERATED CODE -------------- +*/

    /*+ ------------ BEGIN AUTO GENERATED CODE ------------ +*/

    /*+ ------------ END AUTO GENERATED CODE -------------- +*/

    @Override
    public Type[] getServiceTypes()
    {
        // Register this service type
        return new Type[] { TYPE };
    }
    @Override
    public void started() throws Exception {
        super.started();

        try {
            System.out.println("Starting Niagara HTTP/HTTPS Service...");

            // 1. Is certificate configured?
            String certName = getCertificateFileName();
            String certPass = getCertificatePassword();

            if (certName != null && !certName.isEmpty() &&
                    certPass != null && !certPass.isEmpty())
            {
                // Try to load & validate
                File certFile = loadCertificateFromStation(certName);

                if (certFile != null) {
                    try {
                        // Try SSL
                        startHttpsServer();
                        setValidationOut("HTTPS started using certificate: " + certName);
                        return;
                    } catch (Exception sslErr) {
                        // Wrong password or corrupt file
                        sslErr.printStackTrace();
                        setValidationOut("⚠ HTTPS failed: " + sslErr.getMessage());
                    }
                }
            }

            // If here → certificate missing OR invalid → fallback to HTTP
            startServer();
            setValidationOut("Running in HTTP mode (no valid certificate found). Upload certificate to enable HTTPS.");

        } catch (Exception e) {
            e.printStackTrace();
            setValidationOut("Service startup failed: " + e.getMessage());
            startServer(); // last fallback
        }
    }

    @Override
    public void stopped() throws Exception {
        stopServer();
        super.stopped();
    }

    /**
     * Load certificate dynamically from module path
     */
    public File loadCertificateFromStation(String fileName) {
        try {
            // Get the station home directory
            File stationHome = Sys.getStationHome();

            // Folder where certificates will be stored
            File certDir = new File(stationHome, "niagara_certificate");
            if (!certDir.exists()) {
                boolean created = certDir.mkdirs(); // create folder if missing
                if (!created) {
                    System.out.println(" Failed to create certificate folder: " + certDir.getAbsolutePath());
                    return null;
                }
            }

            // Ensure filename ends with .p12
            if (!fileName.endsWith(".p12")) {
                fileName += ".p12";
            }

            File certFile = new File(certDir, fileName);

            if (!certFile.exists()) {
                System.out.println("Certificate file not found: " + certFile.getAbsolutePath());
                return null;
            }

            System.out.println("Loaded certificate from disk: " + certFile.getAbsolutePath());
            return certFile;

        } catch (Exception e) {
            System.out.println("Error loading certificate: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Create SSL Context from P12 file
     * @param certName
     */
    private SSLContext createSSLContext(String certName) throws Exception {
        File p12 = loadCertificateFromStation(certName);
        if (p12 == null) {
            throw new Exception("Certificate file missing!");
        }

        String pass = getCertificatePassword();
        if (pass == null || pass.isEmpty()) {
            throw new Exception("Certificate password is not set in service!");
        }

        char[] password = pass.toCharArray();

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(p12)) {
            ks.load(fis, password);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, password);

        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), null, null);

        return ssl;
    }

    /**
     * Start HTTPS Server
     */
    public void startHttpsServer() throws Exception {
        SSLContext sslContext = createSSLContext(getCertificateFileName());
        HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(8443), 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        this.server = httpsServer;

        // Register routes
        BmsRoute.registerRoutes(server, Sys.getStation().getParentComponent());
        AuthRoute.registerRoutes(server, this);
        LoginRoute.registerRoutes(server);
        ObjectViewerRoute.registerRoutes(server);
        CertificateUploadRoute.registerRoutes(server,this);
        DashboardRoute.registerRoutes(server,this);

        httpsServer.setExecutor(null);
        httpsServer.start();
        System.out.println("HTTPS Niagara Server started at https://localhost:8443/");
    }

    /**
     * Called by CertificateUploadRoute after .p12 upload & password success
     */
    public void upgradeToHttps() throws Exception {
        // Validate certificate first
        String validationResult = validateCertificate(getCertificateFileName(), getCertificatePassword());
        if (!"valid".equals(validationResult)) {
            throw new Exception("Certificate validation failed: " + validationResult);
        }

        System.out.println("Stopping HTTP server...");
        stopServer();

        System.out.println("Starting HTTPS server...");
        startHttpsServer();

        setValidationOut("HTTPS enabled successfully. Certificate is valid.");
        System.out.println("Certificate validation passed.");
    }


    /**
     * Validate a certificate file (.p12) and return "valid" or "invalid"
     */
    public String validateCertificate(String fileName, String password) {
        try {
            File certFile = loadCertificateFromStation(fileName);
            if (certFile == null || !certFile.exists()) {
                return "invalid: file not found";
            }

            if (password == null || password.isEmpty()) {
                return "invalid: password is empty";
            }

            // Try to load the certificate
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(certFile)) {
                ks.load(fis, password.toCharArray());
            }

            // Check if certificate contains at least one entry
            if (ks.size() > 0) {
                return "valid";
            } else {
                return "invalid: empty keystore";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "invalid: " + e.getMessage();
        }
    }


    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(8000), 0);

        // register all routes
        BmsRoute.registerRoutes(server,  Sys.getStation().getParentComponent());

        // Register Auth route for login
        AuthRoute.registerRoutes(server, this);

        // Add Login page route
        LoginRoute.registerRoutes(server);

        // Add ObjectViewer page route
        ObjectViewerRoute.registerRoutes(server);

        CertificateUploadRoute.registerRoutes(server,this);

        DashboardRoute.registerRoutes(server,this);

        server.setExecutor(null);
        server.start();
        System.out.println("✅ Niagara HTTP Server started at http://localhost:8000/");
    }

    public void stopServer() {
        if (server != null) {
            server.stop(0);
            setValidationOut("🛑 Niagara HTTP Server stopped.");
        }
    }

    @Override
    public void changed(Property property, Context context) {
        super.changed(property, context);

        if (property == username || property == password) {
            String user = getUsername();
            String pass = getPassword();
            if (!isValidCredential(user)) {
                System.out.println("Invalid username! Must contain letters, numbers, and symbol, 6-20 chars");
            } else if (!isValidCredential(pass)) {
                System.out.println("Invalid password! Must contain letters, numbers, and symbol, 6-20 chars");
            } else {
                System.out.println("Credentials valid ✔");
            }
        }
    }

    private boolean isValidCredential(String value) {
        if (value == null || value.length() < 6 || value.length() > 20) return false;
        // Must contain at least one letter, one number, and one symbol
        boolean hasLetter = false, hasDigit = false, hasSymbol = false;
        for (char c : value.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSymbol = true;
        }
        return hasLetter && hasDigit && hasSymbol;
    }


}
