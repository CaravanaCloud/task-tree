package tasktree.aws.ec2;

import software.amazon.awssdk.services.ec2.model.DeleteVpcEndpointsRequest;
import software.amazon.awssdk.services.ec2.model.VpcEndpoint;
import tasktree.aws.AWSCleanup;

public class DeleteVPCEndpoint extends AWSCleanup<VpcEndpoint> {
    public DeleteVPCEndpoint(VpcEndpoint resource) {
        super(resource);
    }

    @Override
    public void cleanup(VpcEndpoint resource) {
        log().info("Deleting vpc endpoint {}", resource.vpcEndpointId());
        var request = DeleteVpcEndpointsRequest.builder()
                .vpcEndpointIds(resource.vpcEndpointId())
                .build();
        aws().newEC2Client(getRegion()).deleteVpcEndpoints(request);
    }

    @Override
    protected String getResourceType() {
        return "VPC Endpoint";
    }
}
