package net.easyrpc.rpc.service.api;

public interface DemoService {

    /***
     * @return a * b
     */
    Integer add(Integer a, Integer b);

    /***
     * @return a - b
     */
    Integer min(Integer a, Integer b);

    /***
     * @return (float) a / b
     */
    Float div(Integer a, Integer b);

    /***
     * @return a * b
     */
    Integer mul(Integer a, Integer b);

    /***
     * @return a ^ b
     */
    Integer pow(Integer a, Integer b);

}