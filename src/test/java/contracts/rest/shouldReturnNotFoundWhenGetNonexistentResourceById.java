/*
package contracts.rest;

import org.springframework.cloud.contract.spec.Contract;

import java.util.function.Supplier;

import static org.springframework.cloud.contract.verifier.util.ContractVerifierUtil.map;

public class shouldReturnNotFoundWhenGetNonexistentResourceById implements Supplier<Contract> {
    @Override
    public Contract get() {
        return Contract.make(contract -> {
            contract.description("Represents a not-found-resource scenario of getting a resource by id");
            contract.request(request -> {
                request.method(request.GET());
                request.url("/api/v1/resources/1999");
            });
            contract.response(response -> {
                response.status(404);
                response.body(map()
                        .entry("status", "NOT_FOUND")
                        .entry("message", "Resource not found")
                        .entry("debugMessage", "Resource with id=1999 not found")
                );
            });
        });
    }
}
*/
