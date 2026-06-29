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
        window.setDefaultSize(300, 200);

        var box = Box.builder()
            .setOrientation(Orientation.VERTICAL)
            .setHalign(Align.CENTER)
            .setValign(Align.CENTER)
            .build();

        Label blocksLabel  = Label.builder().setLabel("0").build();
        Label headersLabel = Label.builder().setLabel("0").build();
        Label hashLabel    = Label.builder().setLabel("n/a").build();

        Label peersLabel    = Label.builder().setLabel("-1").build();
        Label bloomLabel    = Label.builder().setLabel("-1").build();
        Label maxPeersLabel    = Label.builder().setLabel("-1").build();

        FxGtkBinding.bindLabel(blocksLabel,  networkModel.blocksProperty(),         Object::toString);
        FxGtkBinding.bindLabel(headersLabel, networkModel.headersProperty(),        Object::toString);
        FxGtkBinding.bindLabel(hashLabel,    networkModel.bestBlockHashProperty(),  Sha256Hash::toString);

        FxGtkBinding.bindLabel(peersLabel,   networkModel.peerCountProperty(),  Object::toString);
        FxGtkBinding.bindLabel(bloomLabel,   networkModel.bloomCountProperty(),  Object::toString);
        FxGtkBinding.bindLabel(maxPeersLabel, networkModel.maxPeersProperty(),  Object::toString);

        box.append(blocksLabel);
        box.append(headersLabel);
        box.append(hashLabel);
        box.append(peersLabel);
        box.append(bloomLabel);
        box.append(maxPeersLabel);

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

        window.setChild(box);
        window.present();
    }

    private void shutdown() {
        peerNetwork.close();
    }
}
