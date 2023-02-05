package contracts.rest;

import org.springframework.cloud.contract.spec.Contract;

import java.util.function.Supplier;

import static org.springframework.cloud.contract.verifier.util.ContractVerifierUtil.map;

public class shouldReturnBadRequestWhenDeleteResources implements Supplier<Contract> {
    @Override
    public Contract get() {
        return Contract.make(contract -> {
            contract.description("Represents a bad request scenario of deleting a resource");
            contract.request(request -> {
                request.url("/api/v1/resources", url -> {
                    url.queryParameters(queryParameters -> {
                        queryParameters.parameter("id", "");
                    });
                });
                request.method(request.DELETE());
            });
            contract.response(response -> {
                response.status(response.BAD_REQUEST());
                response.body(map()
                        .entry("status", "BAD_REQUEST")
                        .entry("message", "Invalid request")
                        .entry("debugMessage", "Id param was not validated, check your file")
                );
            });
        });
    }
}
