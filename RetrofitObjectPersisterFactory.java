package pax.gitrepolist.rest.roboSpice.core;

import android.app.Application;

import com.octo.android.robospice.persistence.exception.CacheCreationException;
import com.octo.android.robospice.persistence.file.InFileObjectPersister;
import com.octo.android.robospice.persistence.file.InFileObjectPersisterFactory;

import java.io.File;

import retrofit2.Converter;
import retrofit2.Retrofit;

public class RetrofitObjectPersisterFactory extends InFileObjectPersisterFactory {

    private Converter.Factory converter;
    private Retrofit          retrofit;

    public RetrofitObjectPersisterFactory(Application application, Converter.Factory converter) throws CacheCreationException {
        super(application);
        this.converter = converter;
    }

    @Override
    public <DATA> InFileObjectPersister<DATA> createInFileObjectPersister(Class<DATA> clazz, File cacheFolder) throws CacheCreationException {
        return new RetrofitObjectPersister<DATA>(getApplication(), converter, clazz, cacheFolder, retrofit);
    }

    public void setRetrofit(Retrofit retrofit) {
        this.retrofit = retrofit;
    }
}
