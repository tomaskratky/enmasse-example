import io.enmasse.systemtest.*;
import io.vertx.core.http.HttpMethod;

import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws Exception {
        //Environment requires set some env parameters like:
        //OPENSHIFT_USER, OPENSHIFT_TOKEN, OPENSHIFT_URL, OPENSHIFT_PROJECT, OPENSHIFT_USE_TLS
        //OPENSHIFT_SERVER_CERT, KEYCLOAK_ADMIN_USER, KEYCLOAK_ADMIN_PASSWORD
        //Those variables are set in deployment scripts, so we shouldn't care about that.
        Environment envVariables = new Environment();
        Kubernetes kubernetes = OpenShift.create(envVariables);
        AddressApiClient apiClient = new AddressApiClient(kubernetes);

        //first you need to create address space (brokered/standard)

        ////////////
        //STANDARD//
        ////////////

        //upper case chars are not allowed
        AddressSpace standardSpaceWithoutAuth = new AddressSpace("standard-address-space", AddressSpaceType.STANDARD, AuthService.NONE);
        apiClient.createAddressSpace(standardSpaceWithoutAuth);
        TestUtils.waitForAddressSpaceReady(apiClient, standardSpaceWithoutAuth.getName());

        //then create address(es)
        //plan of each Destination say you how many resources will be provided for Destination
        Destination standardQueue = Destination.queue("queueInStandard", "sharded-queue");
        Destination standardTopic = Destination.topic("queueInStandard", "sharded-topic");
        Destination anycast = Destination.anycast("queueInStandard", "standard-anycast");
        Destination multicast = Destination.multicast("queueInStandard", "standard-multicast");

        //you can use deploy which includes wait for destinations are ready to use within timeout
        TestUtils.deploy(apiClient, kubernetes, new TimeoutBudget(5, TimeUnit.MINUTES), standardSpaceWithoutAuth, HttpMethod.PUT, standardQueue, standardTopic, anycast, multicast);

        //!!OR!! low level deploy with separated wait for destinations are ready
        //apiClient.deploy(standardSpaceWithoutAuth, HttpMethod.PUT, queue, topic, anycast, multicast);
        //TestUtils.waitForDestinationsReady(apiClient, standardSpaceWithoutAuth, new TimeoutBudget(5, TimeUnit.MINUTES), queue, topic, anycast, multicast);

        ////////////
        //BROKERED//
        ////////////

        //brokered address space can be created similarly
        AddressSpace brokeredSpaceWithoutAuth = new AddressSpace("standard-address-space", AddressSpaceType.BROKERED, AuthService.NONE);
        apiClient.createAddressSpace(brokeredSpaceWithoutAuth);
        TestUtils.waitForAddressSpaceReady(apiClient, brokeredSpaceWithoutAuth.getName());

        //create destination (anycast and multicast are not allowed in brokered address-space)
        Destination brokeredQueue = Destination.queue("queueInStandard", "brokered-queue");
        Destination brokeredTopic = Destination.topic("queueInStandard", "brokered-topic");
        TestUtils.deploy(apiClient, kubernetes, new TimeoutBudget(5, TimeUnit.MINUTES), brokeredSpaceWithoutAuth, HttpMethod.PUT, brokeredQueue, brokeredTopic);


        ///////////////////////////////
        // WITH STANDARD AUTHSERVICE //
        ///////////////////////////////

        AddressSpace brokeredSpaceWithAuth = new AddressSpace("standard-address-space", AddressSpaceType.BROKERED, AuthService.STANDARD);
        apiClient.createAddressSpace(brokeredSpaceWithAuth);
        TestUtils.waitForAddressSpaceReady(apiClient, brokeredSpaceWithAuth.getName());

        //setup keycloak client for creating new user
        KeycloakCredentials creds = envVariables.keycloakCredentials();
        if (creds == null) {
            creds = kubernetes.getKeycloakCredentials();
        }
        KeycloakClient keycloakApiClient = new KeycloakClient(kubernetes.getKeycloakEndpoint(), creds, kubernetes.getKeycloakCA());
        //user with non specified groups (example in command below) has permissions to send/receive from all destinations + permissions for web-console
        keycloakApiClient.createUser(brokeredSpaceWithAuth.getNamespace(), "test_user", "test_user");

    }
}
