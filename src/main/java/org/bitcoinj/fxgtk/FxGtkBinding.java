/*
 * Copyright 2026 M. Sean Gilligan.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bitcoinj.fxgtk;

import javafx.beans.value.ObservableValue;
import org.gnome.glib.GLib;
import org.gnome.gtk.Label;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/// Adapter that binds Gtk Widgets to JavaFX {@link ObservableValue}
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