package org.testcontainers.couchbase;

import com.couchbase.client.core.hooks.CouchbaseCoreSendHook;
import com.couchbase.client.core.lang.Tuple;
import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.message.CouchbaseRequest;
import com.couchbase.client.core.message.CouchbaseResponse;
import com.couchbase.client.core.message.config.BucketConfigRequest;
import com.couchbase.client.core.message.config.BucketConfigResponse;
import com.couchbase.client.core.message.config.ClusterConfigRequest;
import com.couchbase.client.core.message.config.ClusterConfigResponse;
import com.couchbase.client.java.CouchbaseAsyncBucket;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.TranscodingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class CouchbaseInstrumentationHook implements CouchbaseCoreSendHook {
    private final CouchbaseContainer couchbaseContainer;

    @Override
    public Tuple2<CouchbaseRequest, Observable<CouchbaseResponse>>
    beforeSend(CouchbaseRequest originalRequest, Observable<CouchbaseResponse> originalResponse) {
        if ((originalRequest instanceof ClusterConfigRequest)
            || (originalRequest instanceof BucketConfigRequest)) {
            return Tuple.create(originalRequest, originalResponse.map(this::instrumentResponse));
        }
        return Tuple.create(originalRequest, originalResponse);
    }

    private CouchbaseResponse instrumentResponse(CouchbaseResponse couchbaseResponse) {
        if (couchbaseResponse.status().isSuccess()) {
            if (couchbaseResponse instanceof ClusterConfigResponse) {
                return instrumentClusterConfigResponse((ClusterConfigResponse) couchbaseResponse);
            }
            if (couchbaseResponse instanceof BucketConfigResponse) {
                return instrumentBucketConfigResponse((BucketConfigResponse) couchbaseResponse);
            }
        }
        return couchbaseResponse;
    }


    private CouchbaseResponse instrumentClusterConfigResponse(ClusterConfigResponse response) {
        JsonObject json = extractJson(response);

        arrayApply(json.getArray("nodes"), this::instrumentNode);

        return new ClusterConfigResponse(json.toString(), response.status());
    }

    private CouchbaseResponse instrumentBucketConfigResponse(BucketConfigResponse response) {
        JsonObject json = extractJson(response);

        arrayApply(json.getArray("nodes"), this::instrumentNode);
        arrayApply(json.getArray("nodesExt"), this::instrumentNodeExt);
        instrumentServerList(json);

        return new BucketConfigResponse(json.toString(), response.status());
    }

    private void instrumentNode(Object o) {
        JsonObject json = (JsonObject) o;
        JsonObject ports = json.getObject("ports");
        if (ports != null) {
            istrumentPortMap(ports);
        }
    }

    private void instrumentServerList(JsonObject json) {
        JsonObject vBucketServerMap = json.getObject("vBucketServerMap");
        if (vBucketServerMap == null) {
            return;
        }

        List<String> servers = vBucketServerMap.getArray("serverList")
            .toList()
            .stream()
            .map(o -> (String) o)
            .map(this::instrumentServerString)
            .collect(Collectors.toList());

        vBucketServerMap.put("serverList", servers);
    }

    private String instrumentServerString(String s) {
        try {
            int port = Integer.parseInt(s.replace("$HOST:", ""));
            return "$HOST:" + couchbaseContainer.getMappedPort(port);
        } catch (NumberFormatException e) {
            log.warn("Could not extract port from String [{}]", s);
            return s;
        }
    }

    private void arrayApply(JsonArray array, Consumer<? super Object> function) {
        if (array != null) {
            array.forEach(function);
        }
    }

    private void instrumentNodeExt(Object o) {
        JsonObject json = (JsonObject) o;
        JsonObject servicePorts = json.getObject("services");
        if (servicePorts != null) {
            istrumentPortMap(servicePorts);
        }
    }

    private void istrumentPortMap(JsonObject ports) {
        for (String portName : ports.getNames()) {
            try {
                ports.put(portName, couchbaseContainer.getMappedPort(ports.getInt(portName)));
            } catch (IllegalArgumentException e) {
                log.warn("Could not translate port ({}:{}). "
                        + "This might indicate that the couchbase bootstrapping api changed. ",
                    portName, ports.getInt(portName));
            }
        }
    }


    private JsonObject extractJson(ClusterConfigResponse response) {
        return extractJson(response.config());
    }

    private JsonObject extractJson(BucketConfigResponse response) {
        return extractJson(response.config());
    }

    private JsonObject extractJson(String config) {
        try {
            return CouchbaseAsyncBucket.JSON_OBJECT_TRANSCODER.stringToJsonObject(config);
        } catch (Exception e) {
            throw new TranscodingException("Could not decode cluster info.", e);
        }
    }
}
