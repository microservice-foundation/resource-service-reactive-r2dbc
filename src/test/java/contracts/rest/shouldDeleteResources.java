/*
package contracts.rest;

import org.springframework.cloud.contract.spec.Contract;

import java.util.Collections;
import java.util.function.Supplier;

import static org.springframework.cloud.contract.verifier.util.ContractVerifierUtil.map;

public class shouldDeleteResources implements Supplier<Contract> {
    @Override
    public Contract get() {
        return Contract.make(contract -> {
            contract.request(request -> {
                request.url("/api/v1/resources", url -> {
                    url.queryParameters(queryParameters -> {
                        queryParameters.parameter("id", "1");
                    });
                });
                request.method(request.DELETE());
            });
            contract.response(response -> {
                response.status(response.OK());
                response.body(Collections.singletonList(map().entry("id", "1")));
            });
        });
    }
}
*/
