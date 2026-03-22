package singularity.api;

/**
 * 对争夺资源的抽象，通常表现为 **一组** 资源
 * */
public interface Slot {

    /*
    * 获取资源的唯一辨识 key
    * */
    String getKey();
}
