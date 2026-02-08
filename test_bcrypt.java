import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class test_bcrypt {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 数据库中的哈希
        String storedHash = "$2b$12$1uLnox51dsclaN4VP7wQnen64wtIuBZyp98vltJgmOgEoTjC6/En2";
        
        // 测试密码
        String[] passwords = {"chenxi123", "chenxi123!", "admin", ""};
        
        System.out.println("Spring Security BCrypt 测试:");
        System.out.println("=".repeat(60));
        
        for (String pwd : passwords) {
            boolean matches = encoder.matches(pwd, storedHash);
            System.out.println("密码 '" + pwd + "': " + (matches ? "✓ 匹配" : "✗ 不匹配"));
        }
        
        System.out.println("=".repeat(60));
        
        // 生成新哈希对比
        String newHash = encoder.encode("chenxi123");
        System.out.println("\n新生成的哈希: " + newHash);
        System.out.println("验证新哈希: " + encoder.matches("chenxi123", newHash));
    }
}
