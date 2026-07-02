import org.bitcoinj.base.BitcoinNetwork;
import org.bitcoinj.base.Sha256Hash;
import org.bitcoinj.fxgtk.FxGtkBinding;
import org.bitcoinj.jfx.model.NetworkModel;
import org.bitcoinj.jfx.model.PeerNetwork;
import org.bitcoinj.utils.BriefLogFormatter;
import org.gnome.gtk.*;
import org.gnome.gio.ApplicationFlags;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ChainWatcher {
    private final Application app;
    private final NetworkModel networkModel;
    private final PeerNetwork peerNetwork;


    static void main(String[] args) {
        new ChainWatcher(args);
    }

    public ChainWatcher(String[] args) {
        BriefLogFormatter.init(); // Make log output concise.

        networkModel = new NetworkModel(BitcoinNetwork.MAINNET);
        peerNetwork = new PeerNetwork(networkModel);

        app = new Application("org.bitcoij.ChainWatcher", ApplicationFlags.DEFAULT_FLAGS);
        app.onActivate(this::activate);
        app.onShutdown(this::shutdown);
        app.run(args);
    }

    public void activate() {
        var window = new ApplicationWindow(app);
        window.setTitle(networkModel.network().toString());
        window.setDefaultSize(600, 400);

        var css = getCssProvider();

        Gtk.styleContextAddProviderForDisplay(
                window.getDisplay(),
                css,
                Gtk.STYLE_PROVIDER_PRIORITY_APPLICATION
        );

        var box = Box.builder()
            .setOrientation(Orientation.VERTICAL)
            .setSpacing(4)
            .setHexpand(true)
            .setVexpand(true)
            .setHalign(Align.CENTER)
            .setValign(Align.CENTER)
            .build();

        var caption = new Label("Current block height");
        caption.addCssClass("block-height-caption");

        var mainHeightLabel = new Label("0");
        mainHeightLabel.addCssClass("block-height");

        var hashCaption = new Label("Best block hash");
        hashCaption.addCssClass("block-hash-caption");

        var hashLabel = new Label("0");
        hashLabel.addCssClass("block-hash");

        box.append(caption);
        box.append(mainHeightLabel);
        box.append(hashCaption);
        box.append(hashLabel);

        FxGtkBinding.bindLabel(mainHeightLabel,  networkModel.blocksProperty(),         Object::toString);
        FxGtkBinding.bindLabel(hashLabel,    networkModel.bestBlockHashProperty(),  Sha256Hash::toString);

        var statusBar = new CenterBox();
        statusBar.addCssClass("status-bar");

        // "Peers: N / M"
        var peersBox = Box.builder()
                .setOrientation(Orientation.HORIZONTAL)
                .setSpacing(4)
                .build();

        Label peersLabel    = Label.builder().setLabel("0").setCssClasses(new String[]{"status-num"}).build();
        Label maxPeersLabel = Label.builder().setLabel("0").setCssClasses(new String[]{"status-num"}).build();

        FxGtkBinding.bindLabel(peersLabel,   networkModel.peerCountProperty(),  Object::toString);
        FxGtkBinding.bindLabel(maxPeersLabel, networkModel.maxPeersProperty(),  Object::toString);

        peersBox.append(new Label("Peers:"));
        peersBox.append(peersLabel);
        peersBox.append(new Label("/"));
        peersBox.append(maxPeersLabel);

        // "Block: N / M"
        var blockBox = Box.builder()
                .setOrientation(Orientation.HORIZONTAL)
                .setSpacing(4)
                .build();

        Label blocksLabel  = Label.builder().setLabel("0").setCssClasses(new String[]{"status-num"}).build();
        Label headersLabel = Label.builder().setLabel("0").setCssClasses(new String[]{"status-num"}).build();

        FxGtkBinding.bindLabel(blocksLabel,  networkModel.blocksProperty(),         Object::toString);
        FxGtkBinding.bindLabel(headersLabel, networkModel.headersProperty(),        Object::toString);

        blockBox.append(new Label("Block:"));
        blockBox.append(blocksLabel);
        blockBox.append(new Label("/"));
        blockBox.append(headersLabel);

        statusBar.setStartWidget(peersBox);
        statusBar.setEndWidget(blockBox);

        var root = new Box(Orientation.VERTICAL, 0);
        root.append(box);
        root.append(statusBar);

        window.setChild(root);
        window.present();
    }

    private static CssProvider getCssProvider() {
        var css = new CssProvider();
        css.loadFromString("""
            .block-height {
                font-size: 64pt;
                font-weight: bold;
                font-feature-settings: "tnum";   /* tabular numerals - digits don't jiggle */
            }
            .block-height-caption {
                font-size: 11pt;
                opacity: 0.65;
            }
            .block-hash {
                font-size: 14pt;
                font-weight: bold;
                font-family: "JetBrains Mono", "Fira Code", "Cascadia Code", "Source Code Pro", monospace;
                font-feature-settings: "tnum", "zero";   /* tabular numerals - digits don't jiggle */
            }
            .block-hash-caption {
                font-size: 11pt;
                opacity: 0.65;
            }
            .status-bar {
                padding: 4px 10px;
                border-top: 1px solid alpha(currentColor, 0.15);
            }
            """);
        return css;
    }

    private void shutdown() {
        peerNetwork.close();
    }
}
