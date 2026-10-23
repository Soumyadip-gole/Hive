package protocol;

public class HeartBeat implements Payload{
    double cpuLoad;
    double freeMem;
    HeartBeat(){
        cpuLoad=-1;
        freeMem=-1;
    }
    public HeartBeat(double cpuLoad,double freeMem){
        this.cpuLoad=cpuLoad;
        this.freeMem=freeMem;
    }
}