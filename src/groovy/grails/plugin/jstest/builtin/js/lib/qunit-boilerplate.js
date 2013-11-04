Loader.loadBuiltin('js/lib/qunit-1.15.0pre.js');

QUnit.init();
QUnit.config.blocking = false;
QUnit.config.autorun = true;
QUnit.config.updateRate = 0;

QUnit.testDone = function (result) {
	if (result.failed > 0) {
		throw "Test failed: \'" + result.name + "\' in module \'" + result.module + "\' \n"
	} 
}