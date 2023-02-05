package contracts.rest;

import org.springframework.cloud.contract.spec.Contract;
import org.springframework.cloud.contract.spec.internal.DslProperty;
import org.springframework.cloud.contract.spec.internal.Request;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.springframework.cloud.contract.verifier.util.ContractVerifierUtil.map;

public class shouldReturnBadRequestWhenSaveResource implements Supplier<Contract> {
    private static Map<String, DslProperty> namedProps(Request r) {
        Map<String, DslProperty> map = new HashMap<>();
        // name of the file
        map.put("name", r.$(r.c(r.regex(r.nonEmpty())), r.p("bad-request.mp4")));
        // content of the file
        map.put("content", r.$(r.c(r.regex(r.nonEmpty())), r.p(new byte[]{12, 43, 21, 12, 55})));
        // content type for the part
        map.put("contentType", r.$(r.c(r.regex(r.nonEmpty())), r.p("audio/mp4")));
        return map;
    }

    @Override
    public Contract get() {
        return Contract.make(contract -> {
            contract.description("Represents a bad-request scenario of saving a resource");
            contract.request(request -> {
                request.method(request.POST());
                request.url("/api/v1/resources");
                request.multipart(map().entry("multipartFile", request.named(namedProps(request))));
                request.headers(h -> h.contentType("multipart/form-data"));
            });
            contract.response(response -> {
                response.status(response.BAD_REQUEST());
                response.body(map()
                        .entry("status", "BAD_REQUEST")
                        .entry("message", "Invalid request")
                        .entry("debugMessage", "File with name 'bad-request.mp4' was not validated, check your file")
                );
            });
        });
    }
}
