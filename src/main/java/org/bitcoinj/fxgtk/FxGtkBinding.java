package org.bitcoinj.fxgtk;

import javafx.beans.value.ObservableValue;
import org.gnome.glib.GLib;
import org.gnome.gtk.Label;

import java.util.function.Consumer;
import java.util.function.Function;

public final class FxGtkBinding {
    private FxGtkBinding() {}

    /**
     * Observe a JavaFX property and apply each new value via {@code updater} on the GTK main thread.
     * Applies the current value immediately (call this from the GTK thread during UI setup).
     */
    public static <T> void bind(ObservableValue<T> property, Consumer<? super T> updater) {
        updater.accept(property.getValue());
        property.addListener((obs, oldVal, newVal) ->
                GLib.idleAdd(GLib.PRIORITY_DEFAULT_IDLE, () -> {
                    updater.accept(newVal);
                    return false; // SOURCE_REMOVE — one-shot
                })
        );
    }

    /** Convenience for the common case of pushing a formatted value into a Label. */
    public static <T> void bindLabel(Label label,
                                     ObservableValue<T> property,
                                     Function<? super T, String> formatter) {
        bind(property, v -> label.setLabel(formatter.apply(v)));
    }
}