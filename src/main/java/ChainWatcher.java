import org.bitcoinj.base.BitcoinNetwork;
import org.bitcoinj.base.Sha256Hash;
import org.bitcoinj.fxgtk.FxGtkBinding;
import org.bitcoinj.jfx.model.NetworkModel;
import org.bitcoinj.jfx.model.PeerNetwork;
import org.bitcoinj.utils.BriefLogFormatter;
//import org.gnome.adw.ActionRow;
//import org.gnome.adw.Adw;
import org.gnome.gtk.*;
import org.gnome.gio.ApplicationFlags;

public class ChainWatcher {

    static void main(String[] args) {
        new ChainWatcher(args);
    }

    private final Application app;
    private final NetworkModel networkModel;
    private final PeerNetwork peerNetwork;

    public ChainWatcher(String[] args) {
        // Make log output concise.
        BriefLogFormatter.init();

        networkModel = new NetworkModel(BitcoinNetwork.MAINNET);
        peerNetwork = new PeerNetwork(networkModel);

        // Force register Libadwaita widgets and styling before doing anything else
        //Adw.init();
        
        app = new Application("org.bitcoij.ChainWatcher", ApplicationFlags.DEFAULT_FLAGS);
        app.onActivate(this::activate);
        app.onShutdown(this::shutdown);
        app.run(args);
    }

    public void activate() {
        var window = new ApplicationWindow(app);
        window.setTitle(networkModel.network().toString());
        window.setDefaultSize(600, 400);

        // --- CSS for the big block-height label and the status bar chrome ---
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
                font-size: 12pt;
                font-weight: bold;
                font-feature-settings: "tnum";   /* tabular numerals - digits don't jiggle */
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

//        Label blocksLabel = Label.builder().setLabel("0").build();
//        Label headersLabel = Label.builder().setLabel("0").build();

        //Label peersLabel    = Label.builder().setLabel("-1").build();
        Label bloomLabel    = Label.builder().setLabel("-1").build();
        //Label maxPeersLabel    = Label.builder().setLabel("-1").build();

        FxGtkBinding.bindLabel(mainHeightLabel,  networkModel.blocksProperty(),         Object::toString);
        FxGtkBinding.bindLabel(hashLabel,    networkModel.bestBlockHashProperty(),  Sha256Hash::toString);

//        FxGtkBinding.bindLabel(peersLabel,   networkModel.peerCountProperty(),  Object::toString);
//        FxGtkBinding.bindLabel(bloomLabel,   networkModel.bloomCountProperty(),  Object::toString);
//        FxGtkBinding.bindLabel(maxPeersLabel, networkModel.maxPeersProperty(),  Object::toString);

//        box.append(blocksLabel);
//        box.append(headersLabel);
//        box.append(hashLabel);
//        box.append(peersLabel);
//        box.append(bloomLabel);
//        box.append(maxPeersLabel);

        // ActionRows are typically grouped inside a Gtk.ListBox
        //ListBox listBox = new ListBox();
        //listBox.getStyleContext().addProvider(/* ... adwaita boxed-list style ... */);

        //ActionRow nameRow = new ActionRow();
        //nameRow.setTitle("Blocks");     // The constant
        //nameRow.setSubtitle("0");       // The dynamic database value

        //listBox.append(nameRow);

        //box.append(listBox);

// To update it later when new data arrives:
        //nameRow.setSubtitle(newDatabaseValue);

        // --- Status bar ---
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

        // --- Root ---
        var root = new Box(Orientation.VERTICAL, 0);
        root.append(box);
        root.append(statusBar);

        window.setChild(root);
        window.present();
    }

    private void shutdown() {
        peerNetwork.close();
    }
}
