UPDATE sys_user SET password='$2a$10$y/gSxzaWZHsGPwKHdLDORepIv3.oNqkDuPUHJsW8YW36hXPailpT.' WHERE username IN ('admin','testuser','entrepreneur','landlord01','banker01');
DELETE FROM sys_user WHERE username='hashgen_tmp';
SELECT username, password FROM sys_user;
