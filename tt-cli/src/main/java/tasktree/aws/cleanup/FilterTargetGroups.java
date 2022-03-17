package tasktree.aws.cleanup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup;
import tasktree.spi.Task;

import java.util.List;
import java.util.stream.Stream;

public class FilterTargetGroups extends AWSFilter<TargetGroup> {
    private boolean match(TargetGroup resource) {
        var prefix = getAwsCleanupPrefix();
        var match = resource.targetGroupName().startsWith(prefix);
        return match;
    }

    @Override
    protected List<TargetGroup> filterResources() {
        var elb = aws.getELBClientV2(getRegion());
        var resources = elb.describeTargetGroups().targetGroups();
        var matches = resources.stream().filter(this::match).toList();
        return matches;
    }

    @Override
    public Stream<Task> mapSubtasks(TargetGroup resource) {
        return Stream.of(new DeleteTargetGroup(resource));
    }

    @Override
    protected String getResourceType() {
        return "Target Group";
    }

}
