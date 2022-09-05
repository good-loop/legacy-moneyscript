
import Enum from 'easy-enums';
import C from './base/CBase';
import Roles from './base/Roles';
// import LoginWidget from './base/components/LoginWidget';
export default C;

/**
 * app config
 */
C.app = {
	name: "MoneyScript",
	service: "moneyscript",
	dataspace: "good-loop",
	logo: "/img/gl-logo/LogoMark/logo.green.svg"
};

C.TYPES = new Enum("Task User PlanDoc Money");
C.ROLES = new Enum("user admin");
C.CAN = new Enum("admin");

// setup roles
Roles.defineRole(C.ROLES.user, []);
Roles.defineRole(C.ROLES.admin, C.CAN.values);

