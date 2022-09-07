package cloudjanitor.aws.ec2;

import cloudjanitor.Input;
import software.amazon.awssdk.services.ec2.model.NetworkInterface;
import cloudjanitor.aws.AWSFilter;

import javax.enterprise.context.Dependent;

import static cloudjanitor.Output.AWS.NetworkINterfacesMatch;
@Dependent
public class FilterNetworkInterfaces extends AWSFilter {

    private boolean match(NetworkInterface resource) {
        var match = true;
        var vpcId = inputString(Input.AWS.targetVPCId);
        if (vpcId.isPresent()){
            match = match && resource.vpcId().equals(vpcId);
        }
        var prefix = aws().config().filterPrefix();
        if (prefix.isPresent()) {
            match = match || resource.tagSet().stream()
                    .anyMatch(tag -> tag.key().equals("Name")
                            && tag.value().startsWith(prefix.get()));
        }
        trace("Found Network Interface {} {}", matchMark(match), resource);
        return match;
    }

    @Override
    public void apply() {
        var client = aws().ec2();
        var resources = client.describeNetworkInterfaces().networkInterfaces();
        var matches = resources.stream().filter(this::match).toList();
        debug("Matched {}/{} network interfaces",  matches.size(), resources.size());
        success(NetworkINterfacesMatch,  matches);
    }
}