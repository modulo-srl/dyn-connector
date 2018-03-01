/**
 * Simple function to send Ajax requests to a Dyn server
 *
 * @version 1.1
 * @author Modulo srl
 */

function dyn_send(domain, operation, data, callback) {

	var url = 'https://' + domain + '/api/' + operation;

	$.ajax({
		url: url,
		type: 'POST',
		dataType: 'json',
		async: true,
		processData: false,
		data: JSON.stringify(data),

		success: function(data) {
			callback(data);
		}
	})
	.fail(function(data) {
		var response = {
			error: {
				code: -1,
				reason: "ajax call failed"
			}
		};

		callback(response);
	})
	.done(function() {
		// console.log('done');
	});
}
