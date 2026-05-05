package es.pmdm.gymprofit.network;

import es.pmdm.gymprofit.BuildConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = BuildConfig.BASE_URL;

    private static Retrofit retrofit = null;
    private static String token = null;

    public static void setToken(String authToken) {
        token = authToken;
        retrofit = null;
    }

    public static void clearToken() {
        token = null;
        retrofit = null;
    }

    public static Retrofit getClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(logging);

        httpClient.addInterceptor(chain -> {
            Request original = chain.request();
            Request.Builder requestBuilder = original.newBuilder()
                    .header("Content-Type", "application/json");

            if (token != null && !token.isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + token);
            }

            return chain.proceed(requestBuilder.build());
        });

        httpClient.addInterceptor(chain -> {
            try {
                return chain.proceed(chain.request());
            } catch (Exception e) {
                android.util.Log.e("RETROFIT_ERROR", e.getMessage(), e);
                throw e;
            }
        });

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();



        return retrofit;
    }

    public static ApiService getApiService() {
        return getClient().create(ApiService.class);
    }
}
