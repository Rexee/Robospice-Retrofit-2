package pax.gitrepolist.rest.roboSpice.core;

import android.app.Application;

import com.octo.android.robospice.SpiceService;
import com.octo.android.robospice.persistence.CacheManager;
import com.octo.android.robospice.persistence.exception.CacheCreationException;
import com.octo.android.robospice.request.CachedSpiceRequest;
import com.octo.android.robospice.request.listener.RequestListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import pax.gitrepolist.api.GitHubService;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public abstract class RetrofitSpiceService extends SpiceService {
    private Retrofit                       retrofit;
    private Converter.Factory              mConverter;
    private OkHttpClient                   mClient;
    private RetrofitObjectPersisterFactory cachePersister;
    private Map<Class<?>, Object> retrofitInterfaceToServiceMap = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();

        retrofit = new Retrofit.Builder()
                .baseUrl(getServerUrl())
                .client(getClient())
                .addConverterFactory(getConverter())
                .build();

        cachePersister.setRetrofit(retrofit);
    }

    protected abstract String getServerUrl();

    protected Converter.Factory getConverter() {
        if (mConverter == null) {
            mConverter = GsonConverterFactory.create();
        }

        return mConverter;
    }

    protected OkHttpClient getClient() {
        if (mClient == null) {
            OkHttpClient.Builder OkHttpBuilder = new OkHttpClient.Builder();

            Interceptor interceptorOauth = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request request = chain.request();
                    HttpUrl httpUrl = request.url()
                            .newBuilder()
                            .addQueryParameter(GitHubService.ACCESS_TOKEN, GitHubService.ACCESS_TOKEN_VALUE)
                            .build();
                    request = request.newBuilder()
                            .url(httpUrl)
//                            .addHeader(HeadersContract.HEADER_AUTHONRIZATION, O_AUTH_AUTHENTICATION)
                            .build();
                    return chain.proceed(request);
                }
            };
            OkHttpBuilder.addInterceptor(interceptorOauth);

            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(Level.BODY);
            OkHttpBuilder.addInterceptor(interceptor);

            mClient = OkHttpBuilder.build();
        }

        return mClient;
    }

    @SuppressWarnings("unchecked")
    protected <T> T getRetrofitService(Class<T> serviceClass) {
        T service = (T) retrofitInterfaceToServiceMap.get(serviceClass);
        if (service == null) {
            service = retrofit.create(serviceClass);
            retrofitInterfaceToServiceMap.put(serviceClass, service);
        }
        return service;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void addRequest(CachedSpiceRequest<?> request, Set<RequestListener<?>> listRequestListener) {
        if (request.getSpiceRequest() instanceof RetrofitSpiceRequest) {
            RetrofitSpiceRequest retrofitSpiceRequest = (RetrofitSpiceRequest) request.getSpiceRequest();
            retrofitSpiceRequest.setService(getRetrofitService(retrofitSpiceRequest.getRetrofitedInterfaceClass()));
        }
        super.addRequest(request, listRequestListener);
    }

    @Override
    public CacheManager createCacheManager(Application application) throws CacheCreationException {
        CacheManager cacheManager = new CacheManager();
        cachePersister = new RetrofitObjectPersisterFactory(application, getConverter());
        cacheManager.addPersister(cachePersister);
        return cacheManager;
    }
}
