import { GraphicEntityModule } from './entity-module/GraphicEntityModule.js';
import { AnimatedEventModule } from './animations/AnimatedEventModule.js';
import { ToggleModule } from './toggle-module/ToggleModule.js'

export const modules = [
  GraphicEntityModule,
  AnimatedEventModule,
  ToggleModule,
];

export const playerColors = [
  '#007cb0', // cSignalBlue
  '#f7b500', // cSignalYellow
];

// The list of toggles displayed in the options of the viewer
export const options = [
  ToggleModule.defineToggle({
    // The name of the toggle
    // replace "myToggle" by the name of the toggle you want to use
    toggle: 'showLowSensor',
    // The text displayed over the toggle
    title: 'Low IR sensor',
    // The labels for the on/off states of your toggle
    values: {
      'SHOW': true,
      'HIDE': false
    },
    // Default value of your toggle
    default: false
  }),
  ToggleModule.defineToggle({
    // The name of the toggle
    // replace "myToggle" by the name of the toggle you want to use
    toggle: 'showHighSensor',
    // The text displayed over the toggle
    title: 'High IR sensor',
    // The labels for the on/off states of your toggle
    values: {
      'SHOW': true,
      'HIDE': false
    },
    // Default value of your toggle
    default: false
  }),
]
