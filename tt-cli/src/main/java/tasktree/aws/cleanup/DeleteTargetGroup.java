package tasktree.aws.cleanup;

import software.amazon.awssdk.services.elasticloadbalancingv2.model.DeleteTargetGroupRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup;
import tasktree.Configuration;

import java.lang.annotation.Target;

public class DeleteTargetGroup extends AWSDelete<TargetGroup> {
    public DeleteTargetGroup(TargetGroup resource) {
         super(resource);
    }

    @Override
    public void cleanup(TargetGroup resource) {
        log().debug("Deleting Target group {}", resource.targetGroupArn());
        var request = DeleteTargetGroupRequest.builder()
                .targetGroupArn(resource.targetGroupArn())
                .build();
        aws.getELBClientV2(getRegion()).deleteTargetGroup(request);
    }

    @Override
    protected String getResourceType() {
        return "Target Group";
    }


}
