package org.bitcoinj.fxgtk;

import javafx.beans.value.ObservableValue;
import org.gnome.glib.GLib;
import org.gnome.gtk.Label;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public final class FxGtkBinding {
    private FxGtkBinding() {}

    /// Observe a JavaFX property and apply the latest value via `updater`
    /// on the GTK main thread. At most one update is ever pending; bursts of
    /// property changes collapse into a single UI update carrying the newest value.
    public static <T> void bind(ObservableValue<T> property, Consumer<? super T> updater) {
        AtomicReference<T> latest  = new AtomicReference<>(property.getValue());
        AtomicBoolean pending = new AtomicBoolean(false);

        Runnable schedule = () -> {
            if (pending.compareAndSet(false, true)) {
                GLib.idleAdd(GLib.PRIORITY_DEFAULT_IDLE, () -> {
                    pending.set(false);            // clear before read → concurrent
                    // change re-schedules correctly
                    updater.accept(latest.get());
                    return false;                  // G_SOURCE_REMOVE
                });
            }
        };

        schedule.run(); // apply current value on startup
        property.addListener((obs, oldV, newV) -> {
            latest.set(newV);
            schedule.run();
        });
    }

    /// Convenience for the common case of pushing a formatted value into a Label.
    public static <T> void bindLabel(Label label,
                                     ObservableValue<T> property,
                                     Function<? super T, String> formatter) {
        bind(property, v -> label.setLabel(formatter.apply(v)));
    }
}