package cloudjanitor.aws.ec2;

import cloudjanitor.Input;
import cloudjanitor.aws.AWSWrite;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;

import javax.enterprise.context.Dependent;

@Dependent
public class TerminateInstanceTask extends AWSWrite {
    @Override
    public void apply() {
        var instanceId = getInstanceId();
        debug("Terminating instance {} ", instanceId);
        var terminateInstance = TerminateInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();
        var ec2 = aws().ec2();
        ec2.terminateInstances(terminateInstance);
        success();
    }

    private String getInstanceId() {
        return getInputString(Input.AWS.targetInstanceId);
    }

    @Override
    public String toString() {
        return "TerminateInstance{ " +
                "instanceId=" + getInstanceId() +
                " }";
    }
}
