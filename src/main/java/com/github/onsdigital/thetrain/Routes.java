package com.github.onsdigital.thetrain;

import com.github.onsdigital.thetrain.routes.AddFileToTransaction;
import com.github.onsdigital.thetrain.routes.CommitTransaction;
import com.github.onsdigital.thetrain.routes.GetTransaction;
import com.github.onsdigital.thetrain.routes.OpenTransaction;
import com.github.onsdigital.thetrain.routes.RollbackTransaction;
import com.github.onsdigital.thetrain.routes.SendManifest;
import com.github.onsdigital.thetrain.routes.GetContentHash;
import spark.ResponseTransformer;
import spark.Route;

import static spark.Spark.get;
import static spark.Spark.post;

public class Routes {

    public static void register(Beans beans) {
        ResponseTransformer transformer = beans.getResponseTransformer();

        registerPostHandler("/begin", openTransaction(beans), transformer);
        registerPostHandler("/publish", addFiles(beans), transformer);
        registerPostHandler("/commit", commitTransaction(beans), transformer);
        registerPostHandler("/CommitManifest", sendManifest(beans), transformer);
        registerPostHandler("/rollback", rollbackTransaction(beans), transformer);
        registerGetHandler("/transaction", getTransaction(beans), beans.getResponseTransformer());
        registerGetHandler("/contentHash", getContentHash(beans), beans.getResponseTransformer());
    }

    private static Route openTransaction(Beans beans) {
        return new OpenTransaction(beans.getTransactionsService());
    }

    private static Route addFiles(Beans beans) {
        return new AddFileToTransaction(beans.getTransactionsService(), beans.getPublisherService(),
                beans.getFileUploadHelper());
    }

    private static Route commitTransaction(Beans beans) {
        return new CommitTransaction(beans.getTransactionsService(), beans.getPublisherService());
    }

    private static Route sendManifest(Beans beans) {
        return new SendManifest(beans.getTransactionsService(), beans.getPublisherService(), beans.getWebsitePath());
    }

    private static Route rollbackTransaction(Beans beans) {
        return new RollbackTransaction(beans.getTransactionsService(), beans.getPublisherService());
    }

    private static Route getTransaction(Beans beans) {
        return new GetTransaction(beans.getTransactionsService());
    }

    private static Route getContentHash(Beans beans) {
        return new GetContentHash(beans.getTransactionsService(), beans.getContentService());
    }

    private static void registerPostHandler(String uri, Route route, ResponseTransformer transformer) {
        post(uri, route, transformer);
    }

    private static void registerGetHandler(String uri, Route route, ResponseTransformer transformer) {
        get(uri, route, transformer);
    }
}
