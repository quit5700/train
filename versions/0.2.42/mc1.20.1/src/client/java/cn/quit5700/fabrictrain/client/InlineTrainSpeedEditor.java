package cn.quit5700.fabrictrain.client;

import cn.quit5700.fabrictrain.network.TrainClassNetworking;
import cn.quit5700.fabrictrain.train.TrainClass;
import cn.quit5700.fabrictrain.train.TrainSpeedPresentation;
import cn.quit5700.fabrictrain.network.LegacyClientPlayNetworking;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public final class InlineTrainSpeedEditor {
    private final double[] initial = new double[TrainClass.values().length];
    private final String[] drafts = new String[TrainClass.values().length];
    private boolean loaded;
    private boolean requested;
    private Component error;
    private TrainClass errorClass;

    public InlineTrainSpeedEditor() {}

    public InlineTrainSpeedEditor(double freight, double passenger, double express) {
        refresh(freight, passenger, express);
    }

    public void refresh(TrainClassNetworking.OpenPayload payload) {
        refresh(payload.freightSpeedKmh(), payload.passengerSpeedKmh(), payload.expressSpeedKmh());
    }

    private void refresh(double freight, double passenger, double express) {
        accept(TrainClass.CARGO, freight);
        accept(TrainClass.PASSENGER, passenger);
        accept(TrainClass.EXPRESS, express);
        loaded = true;
    }

    private void accept(TrainClass type, double speed) {
        int index = type.ordinal();
        if (!loaded || drafts[index].equals(TrainSpeedPresentation.format(initial[index]))) {
            initial[index] = speed;
            drafts[index] = TrainSpeedPresentation.format(speed);
        }
    }

    public void requestIfNeeded() {
        if (loaded || requested) return;
        requested = true;
        LegacyClientPlayNetworking.send(new TrainClassNetworking.UpdatePayload(TrainClassNetworking.GLOBAL_SPEED_SETTINGS,
                -1, Double.NaN, Double.NaN, Double.NaN, 0));
    }

    public EditBox field(Font font, int x, int y, int width, TrainClass selected) {
        EditBox field = new EditBox(font, x, y, width, 20, typeLabel(selected));
        field.setX(x);
        field.setY(y);
        field.setHint(Component.translatable("screen.fabric_train_simulator.speed.hint"));
        field.setMaxLength(12);
        field.setValue(loaded ? drafts[selected.ordinal()] : "");
        field.setResponder(value -> { drafts[selected.ordinal()] = value; error = null; });
        field.active = loaded;
        return field;
    }

    private static Component typeLabel(TrainClass selected) {
        return Component.translatable("screen.fabric_train_simulator.depot.speed",
                Component.translatable("class.fabric_train_simulator." + selected.name().toLowerCase(java.util.Locale.ROOT)));
    }

    public Component label(TrainClass selected) {
        return error == null ? typeLabel(selected) : error;
    }

    public TrainClass errorClass() { return errorClass; }

    public boolean hasError() { return error != null; }

    public boolean save() {
        if (!loaded) return false;
        double[] changed = new double[initial.length];
        java.util.Arrays.fill(changed, Double.NaN);
        for (int i = 0; i < initial.length; i++) {
            if (drafts[i].equals(TrainSpeedPresentation.format(initial[i]))) continue;
            var invalid = TrainSpeedPresentation.inputError(drafts[i]);
            if (invalid != null) {
                errorClass = TrainClass.values()[i];
                error = Component.translatable(invalid == TrainSpeedPresentation.InputError.OUT_OF_RANGE
                        ? "screen.fabric_train_simulator.speed.out_of_range"
                        : "screen.fabric_train_simulator.speed.invalid_number");
                return false;
            }
            changed[i] = TrainSpeedPresentation.changedValue(drafts[i], initial[i]);
        }
        LegacyClientPlayNetworking.send(new TrainClassNetworking.UpdatePayload(TrainClassNetworking.GLOBAL_SPEED_SETTINGS,
                0, changed[TrainClass.CARGO.ordinal()], changed[TrainClass.PASSENGER.ordinal()],
                changed[TrainClass.EXPRESS.ordinal()], 0));
        for (int i = 0; i < initial.length; i++) if (Double.isFinite(changed[i])) {
            initial[i] = changed[i];
            drafts[i] = TrainSpeedPresentation.format(changed[i]);
        }
        error = null;
        return true;
    }
}
