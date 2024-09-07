package uwu.narumi.deobfuscator.api.execution;

import java.security.AllPermission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;

public class SandboxPolicy extends Policy {
  @Override
  public PermissionCollection getPermissions(ProtectionDomain domain) {
    if (domain.getClassLoader() instanceof SandboxClassLoader) {
      // Remove all permissions from SandboxClassLoader
      return new Permissions();
    } else {
      Permissions permissions = new Permissions();
      permissions.add(new AllPermission());
      return permissions;
    }
  }
}
