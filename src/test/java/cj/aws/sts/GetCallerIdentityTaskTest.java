package cj.aws.sts;

import cj.TaskTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

@QuarkusTest
@Disabled
public class GetCallerIdentityTaskTest extends TaskTest {
    @Inject
    GetCallerIdentityTask getCaller;

    @Test
    public void testGetCaller(){
        //TODO: fix
    }
}
