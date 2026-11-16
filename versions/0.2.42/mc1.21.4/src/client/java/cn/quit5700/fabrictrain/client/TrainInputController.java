package cn.quit5700.fabrictrain.client;

import cn.quit5700.fabrictrain.entity.PassengerCarEntity;
import cn.quit5700.fabrictrain.entity.LocomotiveEntity;
import cn.quit5700.fabrictrain.network.TrainNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class TrainInputController {
    private static boolean lastForward;
    private static boolean lastBrake;

    private TrainInputController() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || (!(client.player.getVehicle() instanceof PassengerCarEntity)
                    && !(client.player.getVehicle() instanceof LocomotiveEntity))) {
                lastForward = false;
                lastBrake = false;
                return;
            }
            boolean forward = client.options.keyUp.isDown();
            boolean brake = client.options.keyDown.isDown();
            if (forward != lastForward || brake != lastBrake) {
                ClientPlayNetworking.send(new TrainNetworking.DrivePayload(forward, brake));
                lastForward = forward;
                lastBrake = brake;
            }
        });
    }
}
