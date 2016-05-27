package net.easyrpc.engine.io.protocol;

/***
 * Request(tcp)请求的编码/解码协议, 该协议为同步协议
 */
public interface RequestProtocol {

    Request antiSerializeRequest(byte[] bytes);

    byte[] serializeRequest(Request request);

    Response antiSerializeResponse(byte[] bytes);

    byte[] serializeResponse(Response response);

    /***
     * 请求模型
     */
    class Request {

        public String tag;

        public byte[] data;

        public Request() {
        }

        public Request(String tag, byte[] data) {
            this.tag = tag;
            this.data = data;
        }
    }

    /***
     * 响应模型
     */
    class Response {

        public long requestId;

        public int status;

        public byte[] data;

        public Response() {
        }

        public Response meta(long requestId) {
            this.requestId = requestId;
            return this;
        }

        public Response(long requestId, int status, byte[] data) {
            this.requestId = requestId;
            this.status = status;
            this.data = data;
        }
    }
}
