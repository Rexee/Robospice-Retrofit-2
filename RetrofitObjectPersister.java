package pax.gitrepolist.rest.roboSpice.core;


import android.app.Application;

import com.octo.android.robospice.persistence.exception.CacheCreationException;
import com.octo.android.robospice.persistence.exception.CacheLoadingException;
import com.octo.android.robospice.persistence.exception.CacheSavingException;
import com.octo.android.robospice.persistence.file.InFileObjectPersister;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.Converter;
import retrofit2.Retrofit;
import roboguice.util.temp.Ln;

public class RetrofitObjectPersister<T> extends InFileObjectPersister<T> {

    // ============================================================================================
    // ATTRIBUTES
    // ============================================================================================

    private final Converter.Factory converter;
    private final Retrofit retrofit;

    // ============================================================================================
    // CONSTRUCTOR
    // ============================================================================================

    public RetrofitObjectPersister(Application application, Converter.Factory converter, Class<T> clazz, File cacheFolder, Retrofit retrofit) throws CacheCreationException {
        super(application, clazz, cacheFolder);
        this.converter = converter;
        this.retrofit = retrofit;
    }


    // ============================================================================================
    // METHODS
    // ============================================================================================

    @Override
    public T saveDataToCacheAndReturnData(final T data, final Object cacheKey) throws CacheSavingException {

        try {
            if (isAsyncSaveEnabled()) {
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        try {
                            saveData(data, cacheKey);
                        } catch (IOException | CacheSavingException e) {
                            Ln.e(e, "An error occured on saving request " + cacheKey + " data asynchronously");
                        }
                    };
                };
                t.start();
            } else {
                saveData(data, cacheKey);
            }
        } catch (CacheSavingException e) {
            throw e;
        } catch (Exception e) {
            throw new CacheSavingException(e);
        }
        return data;
    }

    private void saveData(T data, Object cacheKey) throws IOException, CacheSavingException {
        Annotation[] dummy = new Annotation[0];
        Converter<T, RequestBody> r = retrofit.requestBodyConverter(getHandledClass(), dummy, dummy);
        RequestBody rb = r.convert(data);
        final Buffer buffer = new Buffer();
        rb.writeTo(buffer);

        // transform the content in json to store it in the cache
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(getCacheFile(cacheKey));
            out.write(buffer.readByteArray());
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected T readCacheDataFromFile(File file) throws CacheLoadingException {
        Converter<ResponseBody, ?> r = converter.responseBodyConverter(getHandledClass(), null, retrofit);
        InputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            final byte[] body = IOUtils.toByteArray(fileInputStream);
            ResponseBody rb = ResponseBody.create(MediaType.parse("application/json"), body);

            return (T) r.convert(rb);

        } catch (FileNotFoundException e) {
            // Should not occur (we test before if file exists)
            // Do not throw, file is not cached
            Ln.w("file " + file.getAbsolutePath() + " does not exists", e);
            return null;
        } catch (Exception e) {
            throw new CacheLoadingException(e);
        } finally {
            IOUtils.closeQuietly(fileInputStream);
        }
    }
}
