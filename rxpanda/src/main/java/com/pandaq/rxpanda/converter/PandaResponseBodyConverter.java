package com.pandaq.rxpanda.converter;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.pandaq.rxpanda.HttpCode;
import com.pandaq.rxpanda.RxPanda;
import com.pandaq.rxpanda.annotation.AutoWired;
import com.pandaq.rxpanda.config.HttpGlobalConfig;
import com.pandaq.rxpanda.entity.EmptyData;
import com.pandaq.rxpanda.entity.IApiData;
import com.pandaq.rxpanda.exception.ApiException;
import com.pandaq.rxpanda.exception.ExceptionType;
import com.pandaq.rxpanda.utils.GsonUtil;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.annotations.NonNull;
import okhttp3.ResponseBody;
import retrofit2.Converter;

/**
 * Created by huxinyu on 2018/5/31.
 * Email : panda.h@foxmail.com
 * <p>
 * Description :响应实体解析类,对返回数据实体做去壳处理
 */

public class PandaResponseBodyConverter<T> implements Converter<ResponseBody, T> {

    private Gson gson;
    private TypeAdapter<T> typeAdapter;
    private Class apiDataClazz;
    private Type dataType; //定义的解析类型

    @SuppressWarnings("unchecked")
    PandaResponseBodyConverter(Gson gson, Type dataType) {
        this.gson = gson;
        this.typeAdapter = (TypeAdapter<T>) gson.getAdapter(TypeToken.get(dataType));
        this.dataType = dataType;
    }

    @SuppressWarnings("unchecked")
    PandaResponseBodyConverter(Gson gson, Type dataType, Class<? extends IApiData> clazz) {
        this.gson = gson;
        this.typeAdapter = (TypeAdapter<T>) gson.getAdapter(TypeToken.get(dataType));
        this.apiDataClazz = clazz;
        this.dataType = dataType;
    }

    @Override
    public T convert(@NonNull ResponseBody value) throws IOException {
        String response = value.string();
        if (apiDataClazz == null) {
            apiDataClazz = RxPanda.globalConfig().getApiDataClazz();
        }
        IApiData apiData = gson.fromJson(response, (Type) apiDataClazz);
        /* 如是按约定格式返回数据 apiData 中的 code 是必须的。
         * 因此可以用 code 是否存在来判断数据是否合法
         */
        if (apiData.getCode() == null) {
            try {
                throw new ApiException(HttpCode.FRAME_WORK.SHELL_FORMAT_ERROR, response, response);
            } finally {
                value.close();
            }
        } else {
            String data = apiData.getData() == null ? defaultData() : GsonUtil.gson().toJson(apiData.getData());
            if (!apiData.isSuccess()) {
                ApiException exception = new ApiException(apiData.getCode(), apiData.getMsg(), data);
                exception.setExceptionType(ExceptionType.API);
                throw exception;
            } else {
                try {
                    T notWire = GsonUtil.gson().fromJson(data, dataType);
                    boolean autoWire = notWire.getClass().getAnnotation(AutoWired.class) != null;
                    if (autoWire) {
                        String wireData = GsonUtil.gson().toJson(notWire);
                        return GsonUtil.gson().fromJson(wireData, dataType);
                    } else {
                        return notWire;
                    }
                } catch (Exception e) {
                    if (HttpGlobalConfig.getInstance().isDebug()) {
                        e.printStackTrace();
                        Log.w("errorData: ", response);
                    }
                    // 原始数据解析不通返回 EmptyData对象解析
                    throw new ApiException(apiData.getCode(), "接口数据与本地数据结构不匹配", data);
                } finally {
                    value.close();
                }
            }

        }
    }

    /**
     * 与 DefaultTypeAdapter 配合，处理 data 为 null 或 data 未返回的情况下的数据
     *
     * @return 处理后的 data 填充值
     */
    private String defaultData() {
        String data;
        // 当解析类型为 DefaultTypeAdapter 中的类型时，设置为 null 交给 GsonAdapter 处理对应的空值
        if (Boolean.class.equals(dataType) ||
                Byte.class.equals(dataType) ||
                Short.class.equals(dataType) ||
                Integer.class.equals(dataType) ||
                Long.class.equals(dataType) ||
                Float.class.equals(dataType) ||
                Double.class.equals(dataType) ||
                Number.class.equals(dataType) ||
                String.class.equals(dataType) ||
                BigDecimal.class.equals(dataType) ||
                StringBuilder.class.equals(dataType) ||
                StringBuffer.class.equals(dataType)) {
            data = "null";
        } else if (List.class.getName().equals(getClazzName())) { //解析类型为 List 时返回一个空集合的 gson 数据
            data = GsonUtil.gson().toJson(new ArrayList<>());
        } else {  //为 null 时默认按空对象解析
            data = GsonUtil.gson().toJson(new EmptyData());
        }
        return data;
    }

    /**
     * 获取解析 data 对象的完整类名
     *
     * @return 完整类名
     */
    private String getClazzName() {
        String className = null;
        if (null != dataType) {
            if (dataType instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) dataType;
                Class clz = (Class) pt.getRawType();
                className = clz.getName();
            } else if (dataType instanceof TypeVariable) {
                TypeVariable tType = (TypeVariable) dataType;
                className = tType.getGenericDeclaration().toString();
            } else {
                Class clz = (Class) dataType;
                className = clz.getName();
            }
        }
        return className;
    }

}
