package agent;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import protocol.*;

public class HeartBeatSender {

    OperatingSystemMXBean os;

    HeartBeatSender() {
        os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    }

    public void sendHeartBeat() {
        double cpuLoad = os.getCpuLoad() * 100;
        double freeMemory = os.getFreeMemorySize() / 1_073_741_824.0;
        HeartBeat heartBeat = new HeartBeat(cpuLoad, freeMemory);
        Message message = new Message(MessageType.HeartBeat, heartBeat);
    }
}
