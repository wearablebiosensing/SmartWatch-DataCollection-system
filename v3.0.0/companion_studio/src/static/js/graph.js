var socket = io.connect("http://" + document.domain + ":" + location.port);
console.log(" SOCKET CONNECTION:==== ", document.domain);
console.log(" SOCKET CONNECTION:==== ", location.port);

const maxDataPoints = 300; // Maximum data points to display on each chart

let chart = null;
let currentListener = null;
const gGraphDropdownInput = document.getElementById("graphDropdownInput");
gGraphDropdownInput.addEventListener("change", () => {
  if (chart) {
    chart.destroy();
  }
  if (currentListener) {
    socket.off(currentListener);
  }

  initializeChart(gGraphDropdownInput.value, maxDataPoints);
});

function setupCharts() {
  // Initialize charts for each canvas
  initializeChart("accelerometer", maxDataPoints);
}
setupCharts();

function initializeChart(canvasId, maxDataPoints) {
  var ctx = document.getElementById("chart").getContext("2d");

  chart = new Chart(ctx, {
    type: "line",

    data: {
      labels: [],
      datasets: [{
        label: `${canvasId}`,
        data: [],
        borderWidth: 1,
        borderColor: "#0A1572",
        backgroundColor: "transparent",
      }, ],
    },
    options: {
      scales: {
        x: {
          ticks: {
            color: "#0A1572",
          },
          grid: {
            drawOnChartArea: false,
          },
          display: true,
          ticks: {
            display: false
          }
        },
        y: {
          ticks: {
            color: "#0A1572",
          },
          grid: {
            color: "rgba(0, 0, 0, 0.1)"
          }
        }
      }
    }
  });

  // Dynamically create the event name based on the canvasId and the watchID from local storage.
  const watchID = localStorage.getItem("carewear_watch_id");
  const eventName = `mqtt_data_${canvasId}_${watchID}`;
  currentListener = eventName;

  socket.on(eventName, function(data) {
    if (chart.data.labels.length >= maxDataPoints) {
      chart.data.labels.shift();
      chart.data.datasets.forEach((dataset) => {
        dataset.data.shift();
      });
    }

    // Add the new data point to the chart
    chart.data.labels.push(""); // Add an empty label
    chart.data.datasets[0].data.push(data["data"]); // Use the "data" key from the emitted dictionary

    // Redraw the chart
    chart.update();
  });
}

// var socket = io.connect("http://" + document.domain + ":" + location.port);
// console.log(" SOCKET CONNECTION:==== ",document.domain);
// console.log(" SOCKET CONNECTION:==== ",document.data);
// console.log(" SOCKET CONNECTION:==== ",location.port);

// const maxDataPoints = 300; // Maximum data points to display on each chart

// let chart = null;
// let currentListener = null;
// const gGraphDropdownInput = document.getElementById("graphDropdownInput");
// gGraphDropdownInput.addEventListener("change", () => {
//   if (chart) {
//     chart.destroy();
//   }
//   if (currentListener) {
//     socket.off(currentListener);
//   }

//   initializeChart(gGraphDropdownInput.value, maxDataPoints);
// });

// function setupCharts() {
//   // Initialize charts for each canvas
//   initializeChart("accelerometer", maxDataPoints);
// }
// setupCharts();

// function initializeChart(canvasId, maxDataPoints) {
//   var ctx = document.getElementById("chart").getContext("2d");

//   chart = new Chart(ctx, {
//     type: "line",

//     data: {
//       labels: [],
//       datasets: [
//         {
//           label: `${canvasId}`,
//           data: [],
//           borderWidth: 1,
//           borderColor: "#0A1572", // Chart line color
//           backgroundColor: "transparent",
//         },
//       ],
//     },
//     options: {
//       scales: {
//         x: {
//           ticks: {
//             color: "#0A1572", // Tick label color
//           },
//           grid: {
//             drawOnChartArea: false,
//           },
//           display: true,
//           ticks: {
//             display: false,
//           },
//         },
//         y: {
//           grid: {
//             color: "#0A1572",
//           },
//           ticks: {
//             color: "#0A1572",
//           },
//         },
//       },
//       legend: {
//         labels: {
//           fontColor: "#0A1572", // Adjust legend label color for dark theme
//         },
//       },
//       elements: {
//         line: {
//           tension: 0.4,
//         },
//         point: {
//           radius: 0,
//         },
//       },
//     },
//   });

//   currentListener = "mqtt_data_" + canvasId;
//   socket.on(currentListener, function (msg) {
//     var time = new Date().toLocaleTimeString();
//     if (chart.data.labels.length >= maxDataPoints) {
//       chart.data.labels.shift(); // Remove the oldest label
//       chart.data.datasets.forEach((dataset) => {
//         dataset.data.shift(); // Remove the oldest data point
//       });
//     }
//     chart.data.labels.push(time);
//     chart.data.datasets.forEach((dataset) => {
//       dataset.data.push(msg.data);
//     });
//     chart.update();
//   });
// }