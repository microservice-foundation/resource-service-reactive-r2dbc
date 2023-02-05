package contracts.rest;

import org.springframework.cloud.contract.spec.Contract;

import java.util.function.Supplier;

public class shouldGetResourceById implements Supplier<Contract> {

    @Override
    public Contract get() {
        return Contract.make(contract -> {
            contract.description("Represents a successful scenario of getting a resource by id");
            contract.request(request -> {
                request.method(request.GET());
                request.url("/api/v1/resources/123");
                request.headers(headers -> headers.accept("audio/mpeg"));
            });
            contract.response(response -> {
                response.status(response.OK());
                response.body(response.fileAsBytes("mpthreetest.mp3"));
                response.headers(headers -> headers.contentType("audio/mpeg"));
            });
        });
    }
}