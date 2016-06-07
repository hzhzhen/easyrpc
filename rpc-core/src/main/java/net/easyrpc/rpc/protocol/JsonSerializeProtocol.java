package net.easyrpc.rpc.protocol;

import com.alibaba.fastjson.JSON;
import net.easyrpc.rpc.error.RpcSerializeError;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class JsonSerializeProtocol implements SerializeProtocol {

    private static final Class[] DIRECT_TYPE = new Class[]{
            int.class, Integer.class, int[].class, Integer[].class,
            short.class, Short.class, short[].class, Short[].class,
            long.class, Long.class, long[].class, Long[].class,
            float.class, Float.class, float[].class, Float[].class,
            double.class, Double.class, double[].class, Double[].class,
            byte.class, Byte.class, byte[].class, Byte[].class, String.class
    };
    final static ConcurrentHashMap<String, Class<?>> DIRECT_TYPE_INDEX;

    static {
        DIRECT_TYPE_INDEX = new ConcurrentHashMap<>();
        for (Class type : DIRECT_TYPE) {
            DIRECT_TYPE_INDEX.put(type.getName(), type);
        }
    }

    @Override
    public byte[] serializeRequest(long invokeId, Method method, Object... args) {
        return JSON.toJSONBytes(new RequestWrapper(invokeId, method, args));
    }

    @Override
    public MethodRequest antiSerializeRequest(byte[] requestData) throws RpcSerializeError {
        try {
            RequestWrapper tmp = JSON.parseObject(requestData, RequestWrapper.class);
            MethodRequest request = new MethodRequest();
            request.invokeId = tmp.invokeId;
            request.service = Class.forName(tmp.serviceName);
            request.method = tmp.getMethod();
            request.args = tmp.getParameters();
            return request;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new RpcSerializeError(e.toString());
        }
    }

    @Override
    public byte[] serializeResponseError(long invokeId, Throwable throwable) {
        return JSON.toJSONBytes(new ResponseWrapper(invokeId, null, null, throwable.toString()));
    }

    @Override
    public byte[] serializeResponseResult(long invokeId, Object object) {
        return JSON.toJSONBytes(new ResponseWrapper(invokeId, object.getClass().getName(), object, null));
    }

    @Override
    public ResultResponse antiSerializeResponse(byte[] responseData) throws RpcSerializeError {
        try {
            ResponseWrapper wrapper = JSON.parseObject(responseData, ResponseWrapper.class);
            ResultResponse response = new ResultResponse();
            response.invokeId = wrapper.invokeId;
            if (wrapper.value == null) {
                response.result = null;
            } else if (DIRECT_TYPE_INDEX.containsKey(wrapper.type)) {
                response.result = JSON.parseObject(JSON.toJSONBytes(wrapper.value), DIRECT_TYPE_INDEX.get(wrapper.type));
            } else {
                response.result = JSON.parseObject(JSON.toJSONBytes(wrapper.value), Class.forName(wrapper.type));
            }
            response.error = wrapper.error;
            return response;
        } catch (ClassNotFoundException e) {
            throw new RpcSerializeError(e.toString());
        }
    }

    public static class RequestWrapper implements Serializable {

        public long invokeId;
        public String serviceName;
        public String methodName;
        public ArrayList<Argument> args;

        public RequestWrapper() {
        }

        public RequestWrapper(long invokeId, Method method, Object[] param) {
            this();
            this.invokeId = invokeId;
            this.serviceName = method.getDeclaringClass().getName();
            this.methodName = method.getName();
            this.args = new ArrayList<>();
            for (Object obj : param) {
                args.add(new Argument(obj));
            }
        }

        public Method getMethod() throws ClassNotFoundException, NoSuchMethodException {
            Class[] params = new Class[args.size()];
            for (int i = 0; i < params.length; i++) {
                if (DIRECT_TYPE_INDEX.containsKey(args.get(i).type)) {
                    params[i] = DIRECT_TYPE_INDEX.get(args.get(i).type);
                } else {
                    params[i] = Class.forName(args.get(i).type);
                }
            }
            return Class.forName(serviceName).getMethod(methodName, params);
        }

        public Object[] getParameters() throws ClassNotFoundException {
            Object[] result = new Object[args.size()];
            for (int i = 0; i < result.length; i++) {
                //依照JSONObject和数据类型进行反序列化
                if (DIRECT_TYPE_INDEX.containsKey(args.get(i).type)) {
                    result[i] = JSON.parseObject(JSON.toJSONBytes(args.get(i).value),
                            DIRECT_TYPE_INDEX.get(args.get(i).type));
                } else {
                    result[i] = JSON.parseObject(JSON.toJSONBytes(args.get(i).value),
                            Class.forName(args.get(i).type));
                }
            }
            return result;
        }

        public static class Argument {
            public String type;
            public Object value;

            public Argument() {
            }

            public Argument(Object obj) {
                this.type = obj.getClass().getName();
                this.value = obj;
            }

        }
    }

    public static class ResponseWrapper implements Serializable {

        public long invokeId;
        public String type;
        public Object value;
        public String error;

        public ResponseWrapper() {
        }

        public ResponseWrapper(long invokeId, String type, Object value, String error) {
            this();
            this.invokeId = invokeId;
            this.type = type;
            this.value = value;
            this.error = error;
        }

    }
}
