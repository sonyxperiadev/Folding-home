var context = loadContext();

if (context) {
  if (context.value == 1) {
    context.value = context.value + 1;
  } else {
    process.exit(0);
  }
} else {
  context = {value: 1};
} 

script.on('PAUSE', function () {
  saveContext(context);
});

setTimeout(function() {}, 5000);
