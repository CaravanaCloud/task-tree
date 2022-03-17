package tasktree.aws.cleanup;

import software.amazon.awssdk.services.ec2.model.DeleteVpcEndpointsRequest;
import software.amazon.awssdk.services.ec2.model.VpcEndpoint;
import tasktree.Configuration;

public class DeleteVPCEndpoint extends AWSDelete<VpcEndpoint> {
    public DeleteVPCEndpoint(VpcEndpoint resource) {
        super(resource);
    }

    @Override
    public void cleanup(VpcEndpoint resource) {
        log().info("Deleting vpc endpoint {}", resource.vpcEndpointId());
        var request = DeleteVpcEndpointsRequest.builder()
                .vpcEndpointIds(resource.vpcEndpointId())
                .build();
        newEC2Client().deleteVpcEndpoints(request);
    }

    @Override
    protected String getResourceType() {
        return "VPC Endpoint";
    }
}
