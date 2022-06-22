package apps.smartfwd.src.main.java;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;

@Service
@Command(scope = "onos", name = "smart-fwd",
        description = "smart test command shell")
public class SmartfwdCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "hostId", description = "host Id of source",
            required = false, multiValued = false)
    private int hostId = 0;

    @Override
    protected void doExecute() {
        AppComponent smartfwdService = AbstractShellCommand.get(AppComponent.class);
        smartfwdService.testCommand();
    }
}
