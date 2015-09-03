/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mse;

import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory;
import com.sun.javafx.application.HostServicesDelegate;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author michael
 */
public class HtmlBuilder {

    public void openPageExternally(Config cfg, String url) throws IOException {

        if (!cfg.getDefaultBrowser().isEmpty()) {
            new ProcessBuilder(cfg.getDefaultBrowser(), "file://" + url).start();
        } else {
            new ProcessBuilder("x-www-browser", "file://" + url).start();
        }

    }

}
